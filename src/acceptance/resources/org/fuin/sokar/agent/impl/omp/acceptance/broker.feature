Feature: The brokering path works end to end, without an account

  The vault holds a deliberately fake OpenRouter key, so the provider answers 401. That 401 is the
  proof, not the failure: it is the provider rejecting a bad key rather than the proxy rejecting the
  token, so the relay carried the request and the broker swapped the real credential in on the way
  out. Only a 200 needs an account, which is what the credential feature is for.

  The fake key must not look like a real one: it is searched for inside the container.

  Background:
    Given the suite runs as an unprivileged user

  @slow
  Scenario: the agent is pointed at the broker, holds only a task token, and its requests go through
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    When a script runs "printf sk-or-omp-check-not-a-real-key | sokar vault put openrouter --type api-key"
    Then it exits zero
    Given a project called "brokercheck" of class "guarded" with a file in it
    When a task is started in "brokercheck" for the "omp" agent and left running
    # The agent's own setup file, which is how it was pointed at the broker at all.
    And the task's container runs:
      """
      stat -c '%a %U' /home/agent/.omp/agent/models.yml
      """
    Then its output contains "600 agent"
    When the task's container runs:
      """
      cat /home/agent/.omp/agent/models.yml; env
      """
    Then its output contains "sokar_pt_"
    And its output does not contain "sk-or-omp-check-not-a-real-key"
    # The 401 that follows is the provider's, so the command's own exit says nothing here.
    When the task's container runs:
      """
      timeout 240 omp --print --mode json --model z-ai/glm-4.6 "Reply with exactly the word SOKARLIVE and nothing else." || true
      """
    Then the task's broker saw a request
    When a script runs "sokar project unfollow brokercheck --force"
    Then it exits zero
