# Micro Focus Unified Functional Testing (UFT) Plugin

The _Micro Focus Unified Functional Testing_ (UFT) plugin is a quality automation based plugin. 
It is run during development and deployment to automate the execution of functional tests.

This plugin is a work in progress but it is intended to provide the following steps:

* [x] **UFT Scenario from File system** - Execute a UFT Scenario from the File System
* [x] **UFT Scenario from ALM** - Execute a UFT Scenario from ALM
* [x] **Publish Results to Octane** - Take the output of one of the preceding steps and publish the test results to Octane
* [ ] **Publish Results to HTML** - Take the XML output of one the preceding steps and convert to HTML file so it can be attached in DA

Download the latest version from the _release_ directory and install into Deployment Automation.

Because of the nature of UFT and its integration this plugin only works on Windows based systems.

### Building the plugin

To build the plugin you will need to clone the following repositories (at the same level as the repository):

 - [mavenBuildConfig](https://github.com/sda-community-plugins/mavenBuildConfig)
 - [plugins-build-parent](https://github.com/sda-community-plugins/plugins-build-parent)
 - [air-plugin-build-script](https://github.com/sda-community-plugins/air-plugin-build-script)
 
 and then compile using the following command
 ```
   mvn clean package
 ```  

**Please note: this plugins is provided as a "community" plugin and is not supported by Micro Focus in any way**.
