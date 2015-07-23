/*
 * =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 *  This product is protected by U.S. and international copyright
 *  and intellectual property laws. Pivotal products are covered by
 *  more patents listed at http://www.pivotal.io/patents.
 * ========================================================================+
 */
package com.gemstone.gemfire.management.internal.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.shell.core.ExitShellRequest;

import com.gemstone.gemfire.internal.GemFireVersion;
import com.gemstone.gemfire.internal.PureJavaMode;
import com.gemstone.gemfire.management.internal.cli.i18n.CliStrings;
import com.gemstone.gemfire.management.internal.cli.parser.SyntaxConstants;
import com.gemstone.gemfire.management.internal.cli.shell.Gfsh;
import com.gemstone.gemfire.management.internal.cli.shell.GfshConfig;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Launcher class for : <ul>
 * <li> gfsh 7.0
 * <li> server
 * <li> locator
 * <li> Tools (Pulse, VSD, JConsole, JVisualVM)
 * <li> Running Command Line Interface (CLI) Commands from OS prompt like <ol>
 *   <li><ul>
 *   <li> compact offline-disk-store - Compact an offline disk store. If the disk store is large, additional memory may need to be allocated to the process using the --J=-Xmx??? parameter.
 *   <li> describe offline-disk-store - Display information about an offline disk store.
 *   <li> encrypt password - Encrypt a password for use in data source configuration.
 *   <li> run - Execute a set of GFSH commands. Commands that normally prompt for additional input will instead use default values.
 *   <li> start jconsole - Start the JDK's JConsole tool in a separate process. JConsole will be launched, but connecting to GemFire must be done manually.
 *   <li> start jvisualvm - Start the JDK's Java VisualVM (jvisualvm) tool in a separate process. Java VisualVM will be launched, but connecting to GemFire must be done manually.
 *   <li> start locator - Start a Locator.
 *   <li> start pulse - Open a new window in the default Web browser with the URL for the Pulse application.
 *   <li> start server - Start a GemFire Cache Server.
 *   <li> start vsd - Start VSD in a separate process.
 *   <li> status locator - Display the status of a Locator. Possible statuses are: started, online, offline or not responding.
 *   <li> status server - Display the status of a GemFire Cache Server.
 *   <li> stop locator - Stop a Locator.
 *   <li> stop server - Stop a GemFire Cache Server.
 *   <li> validate offline-disk-store - Scan the contents of a disk store to verify that it has no errors.
 *   <li> version - Display product version information.
 *   </ul></li>
 *   <li> multiple commands specified using an option "-e"
 *  </ol>
 * </ul>
 *
 * @author Abhishek Chaudhari
 * @author David Hoots
 * @since 7.0
 */
public final class Launcher {
  private static final String EXECUTE_OPTION = "execute";
  private static final String HELP_OPTION    = "help";
  private static final String HELP           = CliStrings.HELP;

  private static final String MSG_INVALID_COMMAND_OR_OPTION = "Invalid command or option : {0}." + GfshParser.LINE_SEPARATOR
      + "Use 'gfsh help' to display additional information.";

  private final Set<String> allowedCommandLineCommands;
  private final OptionParser commandLineParser;
  private StartupTimeLogHelper startupTimeLogHelper;

  static {
    // See 47325
    System.setProperty(PureJavaMode.PURE_MODE_PROPERTY, "true");
  }

  public static void main(final String[] args) {
    // first check whether required dependencies exist in the classpath
    // should we start without tomcat/servlet jars?
    String nonExistingDependency = CliUtil.cliDependenciesExist(true);
    if (nonExistingDependency != null) {
      System.err.println("Required (" + nonExistingDependency + ") libraries not found in the classpath. gfsh can't start.");
      return;
    }

    Launcher launcher = new Launcher();
    System.exit(launcher.parseCommandLine(args));
  }

  protected Launcher() {
    this.startupTimeLogHelper = new StartupTimeLogHelper();
    this.allowedCommandLineCommands = new HashSet<String>();

    this.allowedCommandLineCommands.add(CliStrings.ENCRYPT);
    this.allowedCommandLineCommands.add(CliStrings.RUN);
    this.allowedCommandLineCommands.add(CliStrings.START_PULSE);
    this.allowedCommandLineCommands.add(CliStrings.START_JCONSOLE);
    this.allowedCommandLineCommands.add(CliStrings.START_JVISUALVM);
    this.allowedCommandLineCommands.add(CliStrings.START_LOCATOR);
    this.allowedCommandLineCommands.add(CliStrings.START_MANAGER);
    this.allowedCommandLineCommands.add(CliStrings.START_SERVER);
    this.allowedCommandLineCommands.add(CliStrings.START_VSD);
    this.allowedCommandLineCommands.add(CliStrings.STATUS_LOCATOR);
    this.allowedCommandLineCommands.add(CliStrings.STATUS_SERVER);
    this.allowedCommandLineCommands.add(CliStrings.STOP_LOCATOR);
    this.allowedCommandLineCommands.add(CliStrings.STOP_SERVER);
    this.allowedCommandLineCommands.add(CliStrings.VERSION);
    this.allowedCommandLineCommands.add(CliStrings.COMPACT_OFFLINE_DISK_STORE);
    this.allowedCommandLineCommands.add(CliStrings.DESCRIBE_OFFLINE_DISK_STORE);
    this.allowedCommandLineCommands.add(CliStrings.EXPORT_OFFLINE_DISK_STORE);
    this.allowedCommandLineCommands.add(CliStrings.VALIDATE_DISK_STORE);
    this.allowedCommandLineCommands.add(CliStrings.PDX_DELETE_FIELD);
    this.allowedCommandLineCommands.add(CliStrings.PDX_RENAME);
    
    this.commandLineParser = new OptionParser();
    this.commandLineParser.accepts(EXECUTE_OPTION).withOptionalArg().ofType(String.class);
    this.commandLineParser.accepts(HELP_OPTION).withOptionalArg().ofType(Boolean.class);
    this.commandLineParser.posixlyCorrect(false);
  }

  private int parseCommandLineCommand(final String... args) {
    Gfsh gfsh = null;
    try {
      gfsh = Gfsh.getInstance(false, args, new GfshConfig());
      this.startupTimeLogHelper.logStartupTime();
    } catch (ClassNotFoundException cnfex) {
      log(cnfex, gfsh);
    } catch (IOException ioex) {
      log(ioex, gfsh);
    } catch (IllegalStateException isex) {
      System.err.println("ERROR : " + isex.getMessage());
    }

    ExitShellRequest exitRequest = ExitShellRequest.NORMAL_EXIT;
    
    if (gfsh != null) {
      final String commandLineCommand = combineStrings(args);

      if (commandLineCommand.startsWith(HELP)) {
        if (commandLineCommand.equals(HELP)) {
          printUsage(gfsh, System.out);
        } else {
          // help is also available for commands which are not available under allowedCommandLineCommands
          gfsh.executeCommand(commandLineCommand);
        }
      } else {
        boolean commandIsAllowed = false;
        for (String allowedCommandLineCommand : this.allowedCommandLineCommands) {
          if (commandLineCommand.startsWith(allowedCommandLineCommand)) {
            commandIsAllowed = true;
            break;
          }
        }

        if (!commandIsAllowed) {
          System.err.println(CliStrings.format(MSG_INVALID_COMMAND_OR_OPTION, CliUtil.arrayToString(args)));
          exitRequest = ExitShellRequest.FATAL_EXIT;
        } else {
          if (!gfsh.executeCommand(commandLineCommand)) {
              if (gfsh.getLastExecutionStatus() != 0) 
                exitRequest = ExitShellRequest.FATAL_EXIT;
          } else if (gfsh.getLastExecutionStatus() != 0) {
              exitRequest = ExitShellRequest.FATAL_EXIT;
          }
        }
      }
    }
    
    return exitRequest.getExitCode();
  }

  private int parseOptions(final String... args) {
    OptionSet parsedOptions;
    try {
      parsedOptions = this.commandLineParser.parse(args);
    } catch (OptionException e) {
      System.err.println(CliStrings.format(MSG_INVALID_COMMAND_OR_OPTION, CliUtil.arrayToString(args)));
      return ExitShellRequest.FATAL_EXIT.getExitCode();
    }
    boolean launchShell    = true;
    boolean onlyPrintUsage = parsedOptions.has(HELP_OPTION);
    if (parsedOptions.has(EXECUTE_OPTION) || onlyPrintUsage) {
      launchShell = false;
    }

    Gfsh gfsh = null;
    try {
      gfsh = Gfsh.getInstance(launchShell, args, new GfshConfig());
      this.startupTimeLogHelper.logStartupTime();
    } catch (ClassNotFoundException cnfex) {
      log(cnfex, gfsh);
    } catch (IOException ioex) {
      log(ioex, gfsh);
    } catch (IllegalStateException isex) {
      System.err.println("ERROR : " + isex.getMessage());
    }

    ExitShellRequest exitRequest = ExitShellRequest.NORMAL_EXIT;

    if (gfsh != null) {
      try {
        if (launchShell) {
          gfsh.start();
          gfsh.waitForComplete();
          exitRequest = gfsh.getExitShellRequest();
        } else if (onlyPrintUsage) {
          printUsage(gfsh, System.out);
        } else {
          @SuppressWarnings("unchecked")
          List<String> commandsToExecute = (List<String>) parsedOptions.valuesOf(EXECUTE_OPTION);

          // Execute all of the commands in the list, one at a time.
          for (int i = 0; i < commandsToExecute.size() && exitRequest == ExitShellRequest.NORMAL_EXIT; i++) {
            String command = commandsToExecute.get(i);
            System.out.println(GfshParser.LINE_SEPARATOR + "(" + (i + 1) + ") Executing - " + command
                + GfshParser.LINE_SEPARATOR);
            if (!gfsh.executeCommand(command) || gfsh.getLastExecutionStatus() != 0) {
              exitRequest = ExitShellRequest.FATAL_EXIT;
            }
          }
        }
      } catch (InterruptedException iex) {
        log(iex, gfsh);
      }
    }

    return exitRequest.getExitCode();
  }

  private int parseCommandLine(final String... args) {
    if (args.length > 0 && !args[0].startsWith(SyntaxConstants.SHORT_OPTION_SPECIFIER)) {
     return parseCommandLineCommand(args);
    }
    
    return parseOptions(args);
  }

  private void log(Throwable t, Gfsh gfsh) {
    if (!(gfsh != null && gfsh.logToFile(t.getMessage(), t))) {
      t.printStackTrace();
    }
  }

  private String combineStrings(String... strings) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String string : strings) {
      stringBuilder.append(string).append(" ");
    }

    return stringBuilder.toString().trim();
  }

  private void printUsage(final Gfsh gfsh, final PrintStream stream) {
    StringBuilder usageBuilder = new StringBuilder();
    stream.print("Pivotal GemFire(R) v");
    stream.print(GemFireVersion.getGemFireVersion());
    stream.println(" Command Line Shell" + GfshParser.LINE_SEPARATOR);
    stream.println("USAGE");
    stream.println("gfsh [ <command> [option]* | <help> [command] | [--help | -h] | [-e \"<command> [option]*\"]* ]" + GfshParser.LINE_SEPARATOR);
    stream.println("OPTIONS");
    stream.println("-e  Execute a command");
    stream.println(Gfsh.wrapText("Commands may be any that are available from the interactive gfsh prompt.  "
            + "For commands that require a Manager to complete, the first command in the list must be \"connect\".", 1));
    stream.println(GfshParser.LINE_SEPARATOR + "AVAILABLE COMMANDS");
    stream.print(gfsh.obtainHelp("", this.allowedCommandLineCommands));
    stream.println("EXAMPLES");
    stream.println("gfsh");
    stream.println(Gfsh.wrapText("Start GFSH in interactive mode.", 1));
    stream.println("gfsh -h");
    stream.println(Gfsh.wrapText("Displays 'this' help. ('gfsh --help' or 'gfsh help' is equivalent)", 1));
    stream.println("gfsh help start locator");
    stream.println(Gfsh.wrapText("Display help for the \"start locator\" command.", 1));
    stream.println("gfsh start locator --name=locator1");
    stream.println(Gfsh.wrapText("Start a Locator with the name \"locator1\".", 1));
    stream.println("gfsh -e \"connect\" -e \"list members\"");
    stream.println(Gfsh.wrapText(
        "Connect to a running Locator using the default connection information and run the \"list members\" command.", 1));
    stream.println();

    printExecuteUsage(stream);

    stream.print(usageBuilder);
  }

  private void printExecuteUsage(final PrintStream printStream) {
    StringBuilder usageBuilder = new StringBuilder();

    printStream.print(usageBuilder);
  }

  private static class StartupTimeLogHelper {
    private final long start = System.currentTimeMillis();
    private long done;

    public void logStartupTime() {
      done = System.currentTimeMillis();
      LogWrapper.getInstance().info("Startup done in " + ( (done - start) / 1000.0) + " seconds.");
    }

    @Override
    public String toString() {
      return StartupTimeLogHelper.class.getName() + " [start=" + start + ", done=" + done + "]";
    }
  }
}