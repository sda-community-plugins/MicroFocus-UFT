import com.microfocus.application.automation.tools.*

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
final def almServerUrl = props['almServerUrl'].trim().replace(':', '\\:')
final def almUserName = props['almUserName'].trim()
final def almPassword = props['almPassword'].trim()
final def encryptedPassword = EncryptionUtils.Encrypt(almPassword, EncryptionUtils.getSecretKey())
final def almProject = props['almProject'].trim()
final def almDomain = props['almDomain'].trim()
final def almRunMode = props['almRunMode'].trim()
final def almTimeout = props['almTimeout'].trim()
final def almRunHost = props['almRunHost'].trim()
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

    if (verbose) {
        println "Created parameter file: ${paramFile.absolutePath} with contents:"
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
