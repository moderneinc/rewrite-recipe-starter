# ArchConf Migration Engineering Workshop Instructions

In this workshop we will be developing OpenRewrite recipes to automate various aspects of 
software maintenance, migration, and modernization.

We're going to be using the Moderne CLI to run the recipes we develop.
Ideally you will use real repositories from your own projects, but if you don't have them then open source projects can be substituted.

## Setup

1. Download the Moderne CLI from https://app.moderne.io/  
   1. You can log into this with any public github account, if you don't have one can get you a direct link or use homebrew
      1. Homebrew: `brew install moderneinc/moderne/mod --head`
      2. Available as a fat jar on maven central: https://repo1.maven.org/maven2/io/moderne/moderne-cli/3.27.1/moderne-cli-3.27.1.jar
   2. See detailed instructions: https://docs.moderne.io/user-documentation/moderne-cli/getting-started/cli-intro
2. Activate the CLI with the ArchConf license key: `mod config license edit MXxjb21tdW5pdHl8MjAyNTAyMTA=.00CHFQjtyl56DxyHyfu5UKZr+oZxLmnKDArVgZjNbVQJ5NhzyGbH3J31hTru2Jxq+rZXwC2CmO8hV4UYQ5XUDg==`
3. Clone a selection of repositories you are interested in running recipes on into a folder together
4. Run `mod build .` in the folder containing the repositories you want to build LSTs for
5. Install OSS recipe modules with `mod configure recipes jar install <group>:<artifact>:LATEST`
6. Run recipes with `mod run . --recipe=<recipe-name>`

