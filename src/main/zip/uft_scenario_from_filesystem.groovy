// --------------------------------------------------------------------------------
// Execute UFT tests from the filesystem
// --------------------------------------------------------------------------------

import com.serena.air.plugin.uft.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

//
// Set a variable for each of the plugin steps's inputs.
// We can check whether a required input is supplied (the helper will fire an exception if not) and
// if it is of the required type.
//
File workDir = new File('.').canonicalFile
String tests = props.notNull('tests')
boolean parallelRunnerMode = props.optional('parallelRunnerMode')
String parallelRunnerEnvs = props.optional('parallelRunnerEnvs')
String fsUftRunMode = props.optional('fsUftRunMode', "Fast")
String fsTimeout = props.optional('fsTimeout', "-1")
String perScenarioTimeOut = props.optional('PerScenarioTimeOut', "10")
String paramsFileName = props.notNull('paramsFileName')
String resultsFileName = props.notNull('resultsFileName')
boolean debugMode = props.optionalBoolean("debugMode", false)

// are we running on windows?
if (!windows) {
    println("Sorry, this plugin step is only supported on Windows!")
    System.exit(2);
}

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "Tests:\n${tests}"
println "UFT parallel running mode: ${parallelRunnerMode}"
println "UFT parallel running environments:\n${parallelRunnerEnvs}"
println "UFT Run Mode: ${fsUftRunMode}"
println "UFT Timeout: ${fsTimeout}"
println "Per Scenario Timeout: ${perScenarioTimeOut}"
println "Parameters Filename: ${paramsFileName}"
println "Results Filename: ${resultsFileName}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

int exitCode = 0

//
// The main body of the plugin step - wrap it in a try/catch statement for handling any exceptions.
//
try {
    // copy the required exe's if they don't exist in the working directory.
    if (!(new File("${workDir}\\HpToolsLauncher.exe").exists())) {
        def srcFile = new File("${PLUGIN_HOME}\\bin\\HpToolsLauncher.exe").newDataInputStream()
        def destFile = new File("${workDir}\\HpToolsLauncher.exe").newDataOutputStream()
        destFile << srcFile
        srcFile.close()
        destFile.close()
        if (debugMode) {
            println("DEBUG - Copied ${PLUGIN_HOME}\\bin\\HpToolsLauncher.exe to ${workDir}\\HpToolsLauncher.exe")
        }
    }

    def hpToolsLauncher = new File("${workDir}\\HpToolsLauncher.exe")
    if (!hpToolsLauncher.exists()) {
        println("ERROR - Cannot find required executable: ${workDir}\\HpToolsLauncher.exe")
        System.exit(1)
    }

    //
    // Create Parameters File
    //
    def paramFile = new File(workDir, paramsFileName)
    paramFile.withWriter { out ->
        out.println("runType=FileSystem")
        out.println("resultsFilename=${resultsFileName}")
        out.println("controllerPollingInterval=30")
        out.println("displayController=0")
        out.println("PerScenarioTimeOut=${perScenarioTimeOut}")
        if (fsUftRunMode) {
            out.println("fsUftRunMode=${fsUftRunMode}")
        }
        if (fsTimeout) {
            out.println("fsTimeout=${fsTimeout}")
        }
        def testCount = 1
        if (tests) {
            String[] testData = tests.split("\n");
            for (String test : testData) {
                if (!test.startsWith("#")) {
                    out.println("Test${testCount}=${test}")
                }
                testCount++
            }
        }
        if (parallelRunnerMode) {
            out.println("parallelRunnerMode=true")
            if (parallelRunnerEnvs) {
                envCount = 1
                parallelRunnerEnvs.split("\n").each { envVar ->
                    //split out the name
                    def parts = envVar.split("(?<=(^|[^\\\\])(\\\\{2}){0,8})=", 2);
                    def envVarName = parts[0];
                    def envVarValue = parts.size() == 2 ? parts[1] : "";
                    for (int i = 1; i < testCount; i++) {
                        if (debugMode) println("DEBUG - Setting parallel runner env for Test ${i} to: '${envVarName}=${envVarValue}'")
                        out.println("ParallelTest${i}Env${envCount}=${envVarName} \\: ${envVarValue}")
                    }
                    envCount++
                }
            }
        }
    }

    if (debugMode) {
        println "DEBUG - Created parameter file: ${paramFile.absolutePath} with contents:"
        println paramFile.text
    }

    //
    // Build Command Line
    //
    def commandLine = []
    commandLine.add(hpToolsLauncher.absolutePath)

    commandLine.add("-paramfile")
    commandLine.add("\"${paramFile.absolutePath}\"")

    // print out command info
    println("INFO - Executing: ${commandLine.join(' ')}")

    //
    // Launch Process
    //
    final def processBuilder = new ProcessBuilder(commandLine as String[]).directory(workDir)
    final def process = processBuilder.start()
    process.out.close() // close stdin
    process.waitForProcessOutput(System.out, System.err) // forward stdout and stderr
    process.waitFor()

    exitCode = process.exitValue()

    paramFile.delete()

} catch (StepFailedException e) {
    //
    // Catch any exceptions we find and print their details out.
    //
    println "ERROR - ${e.message}"
    // An exit with a non-zero value will be deemed a failure
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode)
