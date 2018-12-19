// --------------------------------------------------------------------------------
// Publish Test Results to ALM Octane
// --------------------------------------------------------------------------------

import com.serena.air.plugin.uft.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool
import com.microfocus.mqm.clt.Settings
import com.microfocus.mqm.clt.TestResultCollectionTool

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
String files = props.notNull('files')
String productAreas = props.optional('productAreas')
String backlogItems = props.optional('backlogItems')
String oServerUrl = props.notNull('oServerUrl')
String oUserName = props.notNull('oUserName')
String oPassword = props.optional('oPassword')
String sharedSpace = props.notNull('sharedSpace')
String workspace = props.notNull('workspace')
String release = props.optional('release')
String tags = props.optional('tags')
boolean skipErrors = props.optionalBoolean('oPassword')
boolean checkResult = props.optionalBoolean('checkResult')
boolean debugMode = props.optionalBoolean("debugMode", false)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "Test Files:\n${files}"
println "Product Area(s): ${productAreas}"
println "Backlog Item(s): ${backlogItems}"
println "Octane Server URL: ${oServerUrl}"
println "Octane User Name: ${oUserName}"
println "Octane Passwordt: ${oPassword}"
println "Shared Space Id: ${sharedSpace}"
println "Workspace Id: ${workspace}"
println "Release id: ${release}"
println "Tags:\n${tags}"
println "Skip Errors: ${skipErrors}"
println "Check Result: ${checkResult}"
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
    Settings settings = new Settings()
    settings.setServer(oServerUrl)
    settings.setUser(oUserName)
    settings.setPassword(oPassword)
    settings.setSharedspace(Integer.parseInt(sharedSpace))
    settings.setWorkspace(Integer.parseInt(workspace))
    if (productAreas) {
        String[] paTmp = productAreas.replaceAll("[ \t]*", "").split(',')
        settings.setProductAreas(paTmp)
    }
    if (backlogItems) {
        String[] biTmp = backlogItems.replaceAll("[ \t]*", "").split(',')
        settings.setBacklogItems(biTmp)
    }
    if (release) settings.setRelease(Integer.parseInt(release))
    if (tags) {
        List<String> tTmp = tags.replaceAll("[ \t]*", "").split('\n')
        settings.setTags(tTmp)
    }
    settings.setSkipErrors(skipErrors)
    settings.setCheckResult(checkResult)

    if (debugMode) {
        println "DEBUG - Collecting test result files:"
    }
    List<String> inputXMLFileNames = new ArrayList<>()
    files.replaceAll("[ \t]*", "").split("\n").each { pattern ->
        scanner = new AntBuilder().fileScanner {
            fileset(dir: workDir.canonicalPath, casesensitive: false) {
                include(name: pattern)
            }
        }
        scanner.each {
            if (debugMode) println "DEBUG - Adding file: ${it}"
            inputXMLFileNames.add(it.canonicalPath)
        }
    }
    settings.setInputXmlFileNames(inputXMLFileNames)

    println "INFO - Uploading test results to ${oServerUrl}:"

    TestResultCollectionTool testResultCollectionTool = new TestResultCollectionTool(settings)
    testResultCollectionTool.collectAndPushTestResults()

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
