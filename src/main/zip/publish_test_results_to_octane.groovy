import com.microfocus.mqm.clt.Settings
import com.microfocus.mqm.clt.TestResultCollectionTool

final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

final def workDir = new File('.').canonicalFile
final def props = new Properties();
final def inputPropsFile = new File(args[0]);
final def inputPropsStream = null;
try {
    inputPropsStream = new FileInputStream(inputPropsFile);
    props.load(inputPropsStream);
}
catch (IOException e) {
    throw new RuntimeException(e);
}
finally {
    inputPropsStream.close();
}

final def files = props['files']
final def productAreas = props['productAreas']?.trim()
final def backlogItems = props['backlogItems']?.trim()
final def oServerUrl = props['oServerUrl']?.trim()
final def oUserName = props['oUserName']?.trim()
final def oPassword = props['oPassword']?.trim()
final def sharedSpace = props['sharedSpace']?.trim()
final def workspace = props['workspace']?.trim()
final def release = props['release']?.trim()
final def tags = props['tags']
final boolean skipErrors = props['skipErrors']
final boolean checkResult = props['checkResult']
final boolean verbose = props['verbose']

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

if (verbose) {
    println "Collecting test result files:"
}
List<String> inputXMLFileNames = new ArrayList<>()
files.replaceAll("[ \t]*", "").split("\n").each { pattern ->
    scanner = new AntBuilder().fileScanner {
        fileset(dir: workDir.canonicalPath, casesensitive: false) {
            include(name: pattern)
        }
    }
    scanner.each {
        if (verbose) println "Adding file: ${it}"
        inputXMLFileNames.add(it.canonicalPath)
    }
}
settings.setInputXmlFileNames(inputXMLFileNames)

println "Uploading test results to ${oServerUrl}:"

TestResultCollectionTool testResultCollectionTool = new TestResultCollectionTool(settings)
testResultCollectionTool.collectAndPushTestResults()

System.exit(0);
