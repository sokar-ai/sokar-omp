Feature: The published package on a clean machine

  Everything before this proved the code was right; this proves that what an operator gets is.
  The packages were installed from the repository by the run that rented this machine, the way an
  operator installs them - and this agent's binary was built in another repository against a
  published contract, so sokar has never heard of it.

  Scenario: sokar is installed and discovers the agent it was never linked against
    When a script runs "sokar --version"
    Then it exits zero
    And a script running "sokar agents" mentions "omp"

  Scenario: a person sees the agent by its name and its label
    Given a terminal on the machine
    When I run "sokar agents"
    Then the terminal shows "omp"
    And the terminal shows "Oh My Pi"

  Scenario: the hooks register for this user
    When a script runs "sokar setup"
    Then it exits zero

  Scenario: doctor says what this machine can do
    Given a terminal on the machine
    When I run "sokar doctor"
    Then the terminal shows "hooks registered"

  Scenario: the installed version is a snapshot the next build supersedes
    # A flat snapshot is the same version every build, so 'apt upgrade' has nothing to do and
    # whoever installed yesterday stays there. Asked of dpkg or rpm, not reasoned about.
    Then the installed package "sokar-agent-omp" is a snapshot that the next build supersedes
