@credential
Feature: Sokar shows the agent waiting for a person, read by the agent's own declaration

  Oh My Pi declares in its YAML what a question looks like on its attached screen. This drives it to one
  at the version this package pins and asks Sokar, never the screen, what it makes of it - so a release
  that words its questions differently fails here, instead of going quiet in the field.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set
    # omp marks a tool call that failed with a cross; three of them is the agent retrying, and stops a wait.
    And a tool failure on the screen looks like "✘ "

  @slow
  Scenario: an attached agent is shown not waiting at its prompt, and waiting once it asks
    # Entered, not typed: its prompt takes a line feed as a new line and submits on Enter. Entered only
    # once it is at work, when its screen is in raw mode and the carriage return reaches it as one.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "asks" of class "guarded" with a file in it
    And a terminal on the machine
    # --model: the model the suite pays for answers, not the agent's own default.
    When I run "sokar task start asks --project asks --repository asks --agent omp --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    Then the "omp" agent in task "asks" of "asks" reaches work without being asked anything
    And sokar shows the "omp" agent in task "asks" of "asks" not waiting for a person
    When I enter "Use your ask tool to ask me whether I prefer red or blue. Do nothing else."
    Then sokar shows the "omp" agent in task "asks" of "asks" waiting for a person
    When a script runs "sokar project unfollow asks --force"
    Then it exits zero
