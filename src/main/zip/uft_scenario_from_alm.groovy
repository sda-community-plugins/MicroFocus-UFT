// --------------------------------------------------------------------------------
// Execute UFT tests from ALM
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
String almServerUrl = props.notNull('almServerUrl')
String almUserName = props.notNull('almUserName')
String almPassword = props.optional('almPassword')
String encryptedPassword = EncryptionUtils.Encrypt(almPassword, EncryptionUtils.getSecretKey())
String almProject = props.notNull('almProject')
String almDomain = props.notNull('almDomain')
String almRunMode = props.optional('almRunMode')
String almRunHost = props.optional('almRunHost')
String almTimeout = props.optional('almTimeout', "10")
String paramsFileName = props.notNull('paramsFileName')
String resultsFileName = props.notNull('resultsFileName')
boolean debugMode = props.optionalBoolean("debugMode", false)

// are we running on windows?
if (!windows) {
    println("Sorry, this plugin step is only supported on Windows!")
    System.exit(-1);
}

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "Test Sets:\n${tests}"
println "ALM Server URL: ${almServerUrl}"
println "ALM User Name: ${almUserName}"
println "ALM Password: ${almPassword}"
println "ALM Project\": ${almProject}"
println "ALM Domain: ${almDomain}"
println "ALM Run Mode: ${almRunMode}"
println "Testing Tool Host: ${almRunHost}"
println "ALM Timeout: ${almTimeout}"
println "Parameters Filename: ${paramsFileName}"
println "Results Filename: ${resultsFileName}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

def exitCode = 0

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
        out.println("runType=Alm")
        out.println("resultsFilename=${resultsFileName}")
        out.println("almRunHost=${almRunHost}")
        out.println("almServerUrl=${almServerUrl}")
        out.println("almTimeout=${almTimeout}")
        out.println("almDomain=${almDomain}")
        out.println("almProject=${almProject}")
        out.println("almUserName=${almUserName}")
        out.println("almPassword=${encryptedPassword}")
        out.println("almRunMode=${almRunMode}")
        def testCount = 1
        if (tests) {
            String[] testData = tests.split("\n");
            for (String test : testData) {
                if (!test.startsWith("#")) {
                    out.println("TestSet${testCount}=${test}")
                }
                testCount++
            }
        }
    }

    if (debugMode) {
        println "DEBUG - Created parameter file: ${paramFile.absolutePath} with contents:"
        println paramFile.text.replaceFirst("almPassword=(.*)", "almPassword=*****")
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

    exitCode = process.exitValue();

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
