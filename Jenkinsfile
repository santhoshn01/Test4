pipeline {
    agent any

    environment {
        ARTIFACTORY_CRED_ID = '64311364-a620-409c-b7b4-9c76f73c1c6b'
        ARTIFACTORY_API_URL = 'http://192.168.100.62:8082/artifactory/api/storage/libs-release-local/com/example/demo'
        ARTIFACTORY_BASE_URL = 'http://192.168.100.62:8082/artifactory/libs-release-local/com/example/demo'
        MULLAI_SERVER = '192.168.100.214'
        EMAIL_RECIPIENTS = 'santhosh_n@chelsoft.com'
        JENKINS_BASE_URL = 'http://192.168.100.92:8080/'
        SONAR_PROJECT_KEY = 'demo'
        SONAR_HOST = 'http://192.168.100.92:9000'
        DB_NAME = 'myappdb_test'
        DB_HOST = '192.168.100.92'
        DB_PORT = '5432'
        PSQL_PATH = 'D:\\PostgreSQL\\16\\bin\\psql.exe'
        SQL_FILE = 'D:\\Test\\JavaTest1\\Test4\\load_test_data.sql'
    }

        triggers {
        cron('0 22 * * 1-5') 
    }
    
        stages {
        stage('Conditional Build Execution') {
            steps {
                script {
                    def branch = env.BRANCH_NAME
                    def isTimer = currentBuild.rawBuild.getCauses().toString().contains('TimerTrigger')

                    if (branch == 'main' && isTimer) {
                        echo "Skipping scheduled build on 'main' branch (manual only)"
                        currentBuild.result = 'NOT_BUILT'
                        error("Aborting scheduled build on 'main'")
                    } else {
                        echo "Proceeding with build for branch: ${branch}"
                    }
                }
            }
        }
        
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Get & Increment Version') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.ARTIFACTORY_CRED_ID,
                                                 usernameVariable: 'ART_USER',
                                                 passwordVariable: 'ART_PASS')]) {
                    script {
                        echo "Getting latest version from Artifactory..."

                        bat """
                        curl -u %ART_USER%:%ART_PASS% -s "${env.ARTIFACTORY_API_URL}" > versions.json
                        """

                        def jsonText = readFile('versions.json')
                        def json = readJSON text: jsonText

                        def versions = json.children.findAll { it.folder }.collect { it.uri.replaceAll('/', '') }
                        echo "Found versions: ${versions}"

                        if (versions.isEmpty()) {
                            error "No versions found in Artifactory."
                        }

                        def sortedVersions = sortVersions(versions)
                        def latestVersion = sortedVersions[-1]
                        echo "Latest version: ${latestVersion}"

                        def parts = latestVersion.tokenize('.').collect { it.toInteger() }
                        while (parts.size() < 4) {
                            parts << 0  // Pad with zeros if less than 4 parts
                            }
                            // Optional: stop at max version
                            if (parts == [100, 100, 100, 100]) {
                                error "Maximum version 100.100.100.100 reached."
                                }
                                // Increment with carry over
                                for (int i = 3; i >= 0; i--) {
                                    if (parts[i] < 100) {
                                        parts[i] += 1
                                        break
                                        } else {
                                            parts[i] = 0
                                            }
                                }
                                def newVersion = parts.join('.')
                                env.NEW_VERSION = newVersion
                                echo "New version: ${env.NEW_VERSION}"


                        def buildGradleText = readFile('build.gradle')
                        def newBuildGradle = buildGradleText.replaceAll(/version\s*=\s*'.*'/, "version = '${env.NEW_VERSION}'")
                        writeFile file: 'build.gradle', text: newBuildGradle
                        echo "Updated build.gradle to version ${env.NEW_VERSION}"
                    }
                }
            }
        }
        
        stage('Check & Create Test DB') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'MullaiDB', usernameVariable: 'DB_USER', passwordVariable: 'DB_PASS')]) {
                    bat """
                    setlocal
                    set PGPASSWORD=%DB_PASS%
                    "%PSQL_PATH%" -h %DB_HOST% -U %DB_USER% -p %DB_PORT% -tc "SELECT 1 FROM pg_database WHERE datname='%DB_NAME%'" | findstr 1 >nul
                    if errorlevel 1 (
                        echo Creating DB: %DB_NAME%
                        "%PSQL_PATH%" -h %DB_HOST% -U %DB_USER% -p %DB_PORT% -c "CREATE DATABASE \"%DB_NAME%\""
                    ) else (
                        echo DB %DB_NAME% already exists.
                    )
                    endlocal
                    """
                }
            }
        }
        
        stage('Build') {
    steps {
        bat './gradlew clean build'
    }
}

stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('sonar') {
            bat """
                ./gradlew sonar ^
                -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                -Dsonar.host.url=%SONAR_HOST% ^
                -Dsonar.token=%SONAR_AUTH_TOKEN%
            """
        }
    }
}


        stage('Generate & Archive Sonar Issues Report') {
            steps {
                withCredentials([string(credentialsId: 'JenkinsHello', variable: 'SONAR_TOKEN')]) {
                    script {
                        def buildDir = "sonar-html/build-${env.BUILD_NUMBER}"

                        // Create folder and download Sonar issues JSON
                        bat """
                            if not exist ${buildDir} mkdir ${buildDir}
                            curl -s -u %SONAR_TOKEN%: "${SONAR_HOST}/api/issues/search?componentKeys=${SONAR_PROJECT_KEY}&resolved=false" -o sonar-issues.json
                        """

                        def json = readJSON file: 'sonar-issues.json'
                        def html = """
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <title>Sonar Issues Report</title>
                            </head>
                            <body>
                            <h2>Sonar Issues Report (Unresolved)</h2>
                            <table border="1" cellspacing="0" cellpadding="6" style="width: 100%; border-collapse: collapse;">
                                <tr>
                                <th style="padding: 8px; background-color: #f2f2f2;">Type</th>
                                <th style="padding: 8px; background-color: #f2f2f2;">Severity</th>
                                <th style="padding: 8px; background-color: #f2f2f2;">Message</th>
                                <th style="padding: 8px; background-color: #f2f2f2;">File</th>
                            </tr>
                            """

                            json.issues.each { issue ->
                                def type = issue.type
                                def severity = issue.severity
                                def message = issue.message
                                def component = issue.component
                                def key = issue.key
                                def line = issue.line ?: ''
                                def componentDisplay = line ? "${component}:${line}" : component
                                def issueUrl = "${env.SONAR_HOST}/project/issues?id=${env.SONAR_PROJECT_KEY}&issues=${key}&open=${key}"

                                html += """
                                <tr>
                                    <td style="padding: 8px;">${type}</td>
                                    <td style="padding: 8px;">${severity}</td>
                                    <td style="padding: 8px;">${message}</td>
                                    <td style="padding: 8px;"><a href="${issueUrl}" target="_self">${componentDisplay}</a></td>
                                </tr>
                                """
                            }

                            html += """
                            </table>
                            </body>
                            </html>
                            """

                            // Write the HTML file
                            writeFile file: "${buildDir}/index.html", text: html

                            // Archive the issues JSON (optional)
                            archiveArtifacts artifacts: 'sonar-issues.json', fingerprint: true

                            // Publish the report to Jenkins dashboard
                            publishHTML(target: [
                                reportDir: "${buildDir}",
                                reportFiles: 'index.html',
                                reportName: "SonarQube Report - Build ${env.BUILD_NUMBER}",
                                alwaysLinkToLastBuild: false,
                                keepAll: true
                            ])
                        }
                    }
                }
            }
        }

        stage('Wait for SonarQube Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                        currentBuild.result = 'FAILURE' 
                        def reportPath = "sonar-html/build-${env.BUILD_NUMBER}/index.html"
                        if (fileExists(reportPath)) {
                                sendStageFailureMail("SonarQube Quality Gate", reportPath)
                            }
                        error "Pipeline aborted due to Quality Gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }
        
                stage('Unit Test') {
            steps {
                bat './gradlew clean test'
                bat 'type build\\test-results\\test\\TEST-com.example.demo.DemoApplicationTests.xml'
            }
            post {
            always {
                junit '**/build/test-results/test/*.xml'
                archiveArtifacts artifacts: '**/build/reports/tests/test/*.html', allowEmptyArchive: true
            }
            failure {
                script {
                    def testReport = "build/reports/tests/test/index.html"
                    sendStageFailureMail("JUnit Test Execution", testReport)
                }
            }
        }
    }
        
                stage('Run E2E Tests') {
            steps {
                bat """
                @echo off
                setlocal

                cd /d D:\\Test\\JavaTest1\\Test4

                set DB_URL=jdbc:postgresql://%TEST_DB_HOST%:%TEST_DB_PORT%/%TEST_DB_NAME%
                set DB_USER=%DB_CRED_USR%
                set DB_PASS=%DB_CRED_PSW%

                echo Running E2E Tests with DB: %DB_URL%

                gradlew.bat e2eTest ^
                    -Dspring.datasource.url=%DB_URL% ^
                    -Dspring.datasource.username=%DB_USER% ^
                    -Dspring.datasource.password=%DB_PASS%

                endlocal
                """
            }
        }

        stage('Generate Code Coverage Report (JaCoCo)') {
            steps {
                bat '.\\gradlew.bat jacocoTestReport'
            }
            post {
            always {
                echo 'Publishing JaCoCo HTML report...'
                publishHTML(target: [
                    reportDir: 'build/jacocoHtml',
                    reportFiles: 'index.html',
                    reportName: 'JaCoCo Coverage Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: false
                ])
            }
            failure {
                script {
                    def jacocoReport = "build/jacocoHtml/index.html"
                    sendStageFailureMail("Code Coverage (JaCoCo)", jacocoReport)
                }
            }
        }
    }

        stage('Approval to Publish') {
            steps {
                script {
            def approvers = 'santhosh_n@chelsoft.com,approver2@yourcompany.com'
            def buildUrl = "${env.JENKINS_BASE_URL}job/${env.JOB_NAME}/${env.BUILD_NUMBER}/input"
            def approvalFile = "${env.WORKSPACE}/approval_status.log"

            // Send initial email (before knowing who will approve)
            emailext(
                to: approvers,
                replyTo: 'santhosh_n@chelsoft.com',
                subject: "Approval Needed: Jenkins Build ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <html>
                    <body>
                        <p>Hi,</p>
                        <p>The Jenkins pipeline is waiting for your <b>approval</b> to publish the artifact to Artifactory.</p>
                        <ul>
                            <li><b>Project:</b> ${env.JOB_NAME}</li>
                            <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
                            <li><b>Version:</b> ${env.NEW_VERSION}</li>
                        </ul>
                        <p>Please <a href="${buildUrl}">click here</a> to approve or deny.</p>
                        <p>Regards,<br/>Jenkins</p>
                    </body>
                    </html>
                """,
                mimeType: 'text/html'
            )

            echo "Approval email sent to: ${approvers}"

            try {
                timeout(time: 45, unit: 'MINUTES') {
                    def userInput = input(
                        message: "Do you approve publishing version ${env.NEW_VERSION}?",
                        submitterParameter: 'SUBMITTER',
                        parameters: [
                            booleanParam(name: 'APPROVE', defaultValue: false, description: 'Approve to continue?')
                        ]
                    )

                    def approvalGranted = userInput['APPROVE']
                    def submitter = userInput['SUBMITTER']

                    if (approvalGranted) {
                        echo "Approval granted by ${submitter}."
                        writeFile file: approvalFile, text: "APPROVED by ${submitter} at ${new Date()}"
                    } else {
                        echo "Approval denied by ${submitter}."
                        writeFile file: approvalFile, text: "DENIED by ${submitter} at ${new Date()}"
                        error("Publishing not approved. Aborting pipeline.")
                    }
                }
            } catch (err) {
                echo "Approval timeout or aborted."
                writeFile file: approvalFile, text: "TIMEOUT or NO RESPONSE at ${new Date()}"
                error("No approval received. Aborting pipeline.")
            }
        }
    }
}

        stage('Publish to Artifactory') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.ARTIFACTORY_CRED_ID,
                    usernameVariable: 'ART_USER',
                    passwordVariable: 'ART_PASS'
                )]) {
                    bat """
                    gradlew.bat shadowJar artifactoryPublish -Partifactory_user=%ART_USER% -Partifactory_password=%ART_PASS%
                    """
                }
            }
        }

        stage('Select Deployment Targets') {
            steps {
                script {
                    def userInput = input message: 'Select deployment targets (check any or none):',
                        parameters: [
                            booleanParam(name: 'DEPLOY_LOCAL', defaultValue: false, description: 'Deploy to LOCAL'),
                            booleanParam(name: 'DEPLOY_MULLAI', defaultValue: false, description: 'Deploy to MULLAI')
                        ]
                    env.DEPLOY_LOCAL = userInput.DEPLOY_LOCAL.toString()
                    env.DEPLOY_MULLAI = userInput.DEPLOY_MULLAI.toString()
                    echo "Deploy LOCAL: ${env.DEPLOY_LOCAL}, Deploy MULLAI: ${env.DEPLOY_MULLAI}"
                }
            }
        }

        stage('Deploy') {
    steps {
        script {
            if (env.DEPLOY_LOCAL == 'true') {
                echo "Deploying to LOCAL..."
                node('LocalMachine') {
                    def deployDir = "C:\\deploy\\myapp"
                    def deployPort = "8098"
                    def timestamp = new Date().format('yyyyMMdd-HHmmss')
                    def backupDir = "${deployDir}\\backup-${timestamp}"
                    deployApp("LOCAL", deployDir, deployPort, backupDir)
                }
            }

            if (env.DEPLOY_MULLAI == 'true') {
                echo "Deploying to MULLAI..."
                node('MullaiMachine') {
                    def dbName = "CV_SCHEMA_Trade_Data"
                    def dbBackupDir = "C:\\Users\\mullaiarun_g\\Desktop\\db_backups"
                    def dbHost = "192.168.100.214"
                    backupPostgresDB(dbBackupDir, dbName, dbHost)

                    def remotePath = "C:\\Users\\mullaiarun_g\\Desktop"
                    def deployPort = "8098"
                    def timestamp = new Date().format('yyyyMMdd-HHmmss')
                    def backupDir = "${remotePath}\\backup-${timestamp}"
                    deployApp("MULLAI", remotePath, deployPort, backupDir)
                }
            }

            if (env.DEPLOY_LOCAL != 'true' && env.DEPLOY_MULLAI != 'true') {
                echo "No deployment targets selected. Skipping deployment."
            }
        }
    }
}


    
    post {
    success {
        echo "Pipeline completed successfully for version ${env.NEW_VERSION}"
        emailext(
            to: "${env.EMAIL_RECIPIENTS}",
            subject: "SUCCESS: Jenkins Build #${env.BUILD_NUMBER} - ${env.JOB_NAME}",
            body: """
Hi,

The pipeline completed successfully.

Project: ${env.JOB_NAME}  
Build: #${env.BUILD_NUMBER}  
Version: ${env.NEW_VERSION}  
Link: ${env.JENKINS_BASE_URL}job/${env.JOB_NAME}/${env.BUILD_NUMBER}/

Regards,  
Jenkins
""",
            mimeType: 'text/plain'
        )
    }
    failure {
        echo "Pipeline failed."
        script {
            if (currentBuild.description != 'MAIL_SENT') {
                emailext(
                    to: "${env.EMAIL_RECIPIENTS}",
                    subject: "FAILURE: Jenkins Build #${env.BUILD_NUMBER} - ${env.JOB_NAME}",
                    body: """
Hi,

The pipeline failed.

Project: ${env.JOB_NAME}  
Build: #${env.BUILD_NUMBER}  
Version: ${env.NEW_VERSION ?: 'UNKNOWN'}  
Link: ${env.JENKINS_BASE_URL}job/${env.JOB_NAME}/${env.BUILD_NUMBER}/

Please review the console output for more details.

Regards,  
Jenkins
""",
                    mimeType: 'text/plain'
                )
            }else {
                echo "Detailed stage failure mail already sent. Skipping general failure mail."
            }
        }
    }
}
}


def deployApp(envName, deployDir, deployPort, backupDir) {
    def jarName = "demo-${env.NEW_VERSION}.jar"
    def downloadUrl = "${env.ARTIFACTORY_BASE_URL}/${env.NEW_VERSION}/${jarName}"
    def jarPath = "${deployDir}\\${jarName}"

    echo "Downloading ${jarName} from ${downloadUrl}"
    powershell "Invoke-WebRequest -Uri '${downloadUrl}' -OutFile '${jarPath}'"

    echo "Backing up existing JAR..."
    bat """
        if not exist "${backupDir}" mkdir "${backupDir}"
        forfiles /p "${deployDir}" /m *.jar /c "cmd /c move @file ${backupDir}\\"
    """

    echo "Killing existing app on port ${deployPort} (if any)..."
    bat "for /f \"tokens=5\" %%a in ('netstat -aon ^| findstr :${deployPort}') do taskkill /F /PID %%a || exit 0"

    echo "Starting new JAR..."
    bat "start /B java -jar \"${jarPath}\" > NUL"

    sleep(time: 10, unit: 'SECONDS')

    echo "Checking if app started on port ${deployPort}..."
    def portCheck = bat(script: "netstat -aon | findstr :${deployPort}", returnStatus: true)

    if (portCheck != 0) {
        echo "App failed to start. Performing rollback..."

        // Rollback JAR
        def latestBackup = findLatestBackup(deployDir)
        if (latestBackup) {
            echo "Rolling back JAR from ${latestBackup}"
            bat """
                forfiles /p "${deployDir}" /m *.jar /c "cmd /c del @file"
                copy "${latestBackup}\\*.jar" "${deployDir}\\"
                start /B java -jar "${deployDir}\\*.jar" > NUL
            """
        } else {
            echo "No JAR backup found. Skipping JAR rollback."
        }

        error("Deployment failed and rollback performed.")
    } else {
        echo "Application started successfully on port ${deployPort}."
    }
}

def backupPostgresDB(String backupDir, String dbName, String dbHost) {
    def timestamp = new Date().format('yyyyMMdd-HHmmss')
    def dbBackupFile = "${backupDir}\\pg-backup-${dbName}-${timestamp}.dump"
    def pgDumpExe = "\"C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe\""

    withCredentials([usernamePassword(
        credentialsId: 'MullaiDB',
        usernameVariable: 'DB_USER',
        passwordVariable: 'DB_PASS'
    )]) {
        bat """
            @echo off
            if not exist "${backupDir}" mkdir "${backupDir}"
            set PGPASSWORD=%DB_PASS%
            echo Backing up PostgreSQL DB '%DB_USER%@${dbHost}:5432' (${dbName}) to: ${dbBackupFile}
            ${pgDumpExe} -h ${dbHost} -p 5432 -U %DB_USER% -F c -f "${dbBackupFile}" ${dbName}
            if %ERRORLEVEL% NEQ 0 (
                echo ERROR: PostgreSQL backup failed!
                exit /b 1
            ) else (
                echo ✅ PostgreSQL DB backup successful: ${dbBackupFile}
            )

            REM --- 🔁 Keep only last 5 backup files ---
            pushd "${backupDir}"
            setlocal EnableDelayedExpansion
            set count=0

            for /f %%F in ('dir /b /a:-d /o-d "pg-backup-${dbName}-*.dump"') do (
                set /a count+=1
                if !count! GTR 5 (
                    echo Deleting old backup: %%F
                    del "%%F"
                )
            )

            if !count! LEQ 5 (
                echo No old backups to delete. Total files found: !count!
            )

            endlocal
            popd
        """
    }
}

def rollbackJar(deployDir, deployPort, backupRootDir) {
    echo "Rolling back deployment from latest backup..."

    def rollbackScript = """
        @echo off
        setlocal enabledelayedexpansion

        set "deployDir=${deployDir}"
        set "port=${deployPort}"
        set "backupRoot=${backupRootDir}"

        REM Delete broken JAR
        del /F /Q "!deployDir!\\*.jar" >nul 2>&1

        REM Find latest backup directory
        set "latestBackup="
        for /f "delims=" %%D in ('dir /b /ad /o-d "!backupRoot!" ^| findstr /R "^backup-[0-9]*"') do (
            if not defined latestBackup set "latestBackup=%%D"
        )

        if defined latestBackup (
            echo Restoring from backup: !latestBackup!
            copy /Y "!backupRoot!\\!latestBackup!\\*.jar" "!deployDir!\\"

            for %%F in (!deployDir!\\*.jar) do (
                start "" java -jar "%%F" --server.port=!port! > "!deployDir!\\app.log" 2>&1
            )
        ) else (
            echo ERROR: No valid backup found!
            exit /b 1
        )

        endlocal
    """

    bat rollbackScript
}

def sendStageFailureMail(String stageName, String reportPath) {
    emailext(
        subject: "${stageName} Failed - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """<p>Hi Team,</p>
                 <p>The <strong>${stageName}</strong> failed in <strong>${env.JOB_NAME} #${env.BUILD_NUMBER}</strong>.</p>
                 <p><a href="${env.BUILD_URL}artifact/${reportPath}">Click here</a> to view the report.</p>
                 <p>Regards,<br/>Jenkins</p>""",
        mimeType: 'text/html',
        attachmentsPattern: reportPath,
        to: 'santhosh_n@chelsoft.com'
    )
    currentBuild.description = "MAIL_SENT"
}


@NonCPS
def sortVersions(List<String> list) {
    list.sort { a, b ->
        def tokenize = { v -> v.tokenize('.')*.toInteger() }
        def aParts = tokenize(a)
        def bParts = tokenize(b)
        for (int i = 0; i < Math.max(aParts.size(), bParts.size()); i++) {
            def aVal = i < aParts.size() ? aParts[i] : 0
            def bVal = i < bParts.size() ? bParts[i] : 0
            def cmp = aVal <=> bVal
            if (cmp != 0) return cmp
        }
        return 0
    }
    return list
}