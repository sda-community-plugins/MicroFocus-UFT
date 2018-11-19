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

final def tests = props['tests']
final boolean parallelRunnerMode = props['parallelRunnerMode']
final def parallelRunnerEnvs = props['parallelRunnerEnvs']?.trim()
final def fsUftRunMode = props['fsUftRunMode']?.trim()
final def fsTimeout = props['fsTimeout']?.trim()
final def PerScenarioTimeOut = props['PerScenarioTimeOut']?.trim()
final def paramsFileName = props['paramsFileName']?.trim()
final def resultsFileName = props['resultsFileName']?.trim()
final boolean verbose = props['verbose']

// are we running on windows?
if (!windows) {
    println("Sorry, this plugin step is only supported on Windows!")
    System.exit(-1);
}

// copy the required exe's if they don't exist in the working directory.
if (!(new File("${workDir}\\HpToolsLauncher.exe").exists())) {
    def srcFile = new File("${PLUGIN_HOME}\\bin\\HpToolsLauncher.exe").newDataInputStream()
    def destFile = new File("${workDir}\\HpToolsLauncher.exe").newDataOutputStream()
    destFile << srcFile
    srcFile.close()
    destFile.close()
    if (verbose) {
        println("Copied ${PLUGIN_HOME}\\bin\\HpToolsLauncher.exe to ${workDir}\\HpToolsLauncher.exe")
    }
}

def hpToolsLauncher = new File("${workDir}\\HpToolsLauncher.exe")
if (!hpToolsLauncher.exists()) {
    println("Cannot find required executable: ${workDir}\\HpToolsLauncher.exe")
    System.exit(-1)
}

// create unique parameters file
//def sdfParms = new SimpleDateFormat("'props'ddMMyyhhmmssSSS.'txt'")
//String paramFileName = sdfParms.format(new Date()).toString()
//def sdfResults = new SimpleDateFormat("'Results'ddMMyyhhmmssSSS.'xml'")
//String resultsFileName = sdfResults.format(new Date()).toString().toString()

int exitCode = -1;
try {

    //
    // Create Parameters File
    //
    def paramFile = new File(workDir, paramsFileName)
    paramFile.withWriter { out ->
        out.println("runType=FileSystem")
        out.println("resultsFilename=${resultsFileName}")
        out.println("controllerPollingInterval=30")
        out.println("displayController=0")
        out.println("PerScenarioTimeOut=${PerScenarioTimeOut}")
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
                        if (verbose) println("Setting parallel runner env for Test ${i} to: '${envVarName}=${envVarValue}'")
                        out.println("ParallelTest${i}Env${envCount}=${envVarName} \\: ${envVarValue}")
                    }
                    envCount++
                }
            }
        }
    }

    if (verbose) {
        println "Created parameter file: ${paramFile.absolutePath} with contents:"
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
    println("Executing: ${commandLine.join(' ')}")

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
} finally {

}

System.exit(exitCode);
