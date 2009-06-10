package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

class CommandCallBuilder {
    private final MavenProject defaultProject;
    private final Map<String, MavenProject> modules;
    private final Map<String, String> userAliases;

    public CommandCallBuilder(MavenProject project, Map<String, MavenProject> modules,
            Map<String, String> userAliases) {
        defaultProject = project;
        this.modules = modules;
        this.userAliases = userAliases;
    }
    public void parseCommand(String text, List<CommandCall> commands) {
        List<String> tokens = new ArrayList<String>(Arrays.asList(text.split(" ")));

        // resolve aliases
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (StringUtils.isEmpty(token)) {
                tokens.remove(i);
                continue;
            } else if (userAliases.containsKey(token)) {
                String alias = userAliases.get(token);
                List<String> aliasTokens = new ArrayList<String>(Arrays.asList(alias.split(" ")));
                tokens.remove(i);
                for (Iterator<String> aliasIter = aliasTokens.iterator(); aliasIter.hasNext();) {
                    if (StringUtils.isEmpty(aliasIter.next())) {
                        aliasIter.remove();
                    }
                }
                tokens.addAll(i, aliasTokens);
            } else {
                i++;
            }
        }

        CommandCall currentCommandCall = null;
        for (String token : tokens) {
            if (modules.containsKey(token)) {
                currentCommandCall = addProject(commands, currentCommandCall,
                        modules.get(token));
            } else if (token.contains("*")) {
                String regexToken = token.replaceAll("\\*", ".*");
                for (String moduleName : modules.keySet()) {
                    if (Pattern.matches(regexToken, moduleName)) {
                        currentCommandCall = addProject(commands,
                                currentCommandCall, modules.get(moduleName));
                    }
                }
            } else if (token.equals("-o")) {
                goOffline(commands, currentCommandCall);
            } else if (token.equals("-N")) {
                disableRecursive(commands, currentCommandCall);
            } else if (token.equals("-S")) {
                addProperty(commands, currentCommandCall, "-Dmaven.test.skip=true");
            } else if (token.startsWith("-D")) {
                addProperty(commands, currentCommandCall, token);
            } else if (token.startsWith("-P")) {
                addProfile(commands, currentCommandCall, token);
            } else {
                currentCommandCall = addCommand(commands, currentCommandCall,
                        token);
            }
        }
    }

    private CommandCall addProject(List<CommandCall> commands,
                                   CommandCall currentCommandCall, MavenProject project) {
        if (currentCommandCall == null
                || !currentCommandCall.getCommands().isEmpty()) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.getProjects().add(project);
        return currentCommandCall;
    }

    private CommandCall addCommand(List<CommandCall> commands,
                                   CommandCall currentCommandCall, String command) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            currentCommandCall.getProjects().add(defaultProject);
            commands.add(currentCommandCall);
        }
        currentCommandCall.getCommands().add(command);
        return currentCommandCall;
    }

    private CommandCall disableRecursive(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.doNotRecurse();
        return currentCommandCall;
    }

    private CommandCall goOffline(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.goOffline();
        return currentCommandCall;
    }

    private CommandCall addProfile(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String profile) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        // must have characters after -P
        if (profile.length() < 3) {
            return currentCommandCall;
        }

        profile = profile.substring(2);
        currentCommandCall.getProfiles().add(profile);
        return currentCommandCall;
    }

    private CommandCall addProperty(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String property) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        // must have characters after -D
        if (property.length() < 3) {
            return currentCommandCall;
        }

        property = property.substring(2);
        String key = property;
        String value = "1";
        if (property.indexOf("=") >= 0) {
            String[] propertyTokens = property.split("=");
            key = propertyTokens[0];
            if (propertyTokens.length > 1) {
                value = propertyTokens[1];
            }
        }
        currentCommandCall.getProperties().put(key, value);
        return currentCommandCall;
    }
}
