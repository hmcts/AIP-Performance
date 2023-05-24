# AIP-Performance

Description of AIP Project

This release of IAC is to delivery the additional capability for Appellants-in Person (private individuals) to submit an appeal, 
upload supporting documentation and track appeal status. 
The Appellants will access the system using IAC appellants UI. The workflow for Legal reps, Home Office, 
Case Workers to manage and progress cases, Judiciary to review the case remains unchanged.
1. Appellant uses Submit Appeal web application to submit and appeal. (Private beta url :  www.appeal-asylum-immigration-decision.hmcts.net)
2. Manage appeal web application allows appellant to view case, provide further evidence, upload supporting documents.
3. IDAM is used to authenticate the user credentials
4. Application submitted by appellants via Submit appeals are stored in CCD
5. Documents uploaded through manage appeal are stored into Document store and also updates CCD with url of the stored document
   Notify is used to send notification of case creation and progression of the case


To run locally:
- Performance test against the perftest environment: `./gradlew gatlingRun`

Flags:
- Debug (single-user mode): `-Ddebug=on e.g. ./gradlew gatlingRun -Ddebug=on`
- Run against AAT: `Denv=aat e.g. ./gradlew gatlingRun -Denv=aat`

Before running locally, update the client secret in src/gatling/resources/application.conf then run `git update-index --assume-unchanged src/gatling/resources/application.conf` to ensure the changes aren't pushed to github.

To make other configuration changes to the file, first run `git update-index --no-assume-unchanged src/gatling/resources/application.conf`, ensuring to remove the client secret before pushing to origin
