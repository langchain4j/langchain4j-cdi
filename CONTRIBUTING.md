Thank you for investing your time and effort in contributing to our project, we appreciate it a lot! 🤗

# General guidelines

- If you want to contribute a bug fix or a new feature that isn't listed in the [issues](https://github.com/langchain4j/langchain4j-cdi/issues) yet, please open a new issue for it. We will prioritize is shortly.
- Follow [Google's Best Practices for Java Libraries](https://jlbp.dev/)
- Keep the code compatible with Java 17.
- Avoid adding new dependencies as much as possible (new dependencies with test scope are OK). If absolutely necessary, try to use the same libraries which are already used in the project. Make sure you run `mvn dependency:analyze` to identify unnecessary dependencies.
- Write unit and/or integration tests for your code. This is critical: no tests, no review!
- The tests should cover both positive and negative cases.
- Make sure you run all unit tests on all modules with `mvn clean test`
- Avoid making breaking changes. Always keep backward compatibility in mind. For example, instead of removing fields/methods/etc, mark them `@Deprecated` and make sure they still work as before.
- Follow existing naming conventions.
- Avoid using Lombok in the new code, and remove it from the old code if you get a chance.
- Add Javadoc where necessary. There's no need to duplicate Javadoc from the implemented interfaces.
- Follow existing code style present in the project. Run `make lint` and `make format` before commit.
- Large features should be discussed with maintainers before implementation.

# Priorities

All [issues](https://github.com/langchain4j/langchain4j-cdi/issues) are prioritized by maintainers. There are 4 priorities: [P1](https://github.com/langchain4j/langchain4j-cdi/issues?q=is%3Aissue+is%3Aopen+label%3AP1), [P2](https://github.com/langchain4j/langchain4j-cdi/issues?q=is%3Aissue+is%3Aopen+label%3AP2), [P3](https://github.com/langchain4j/langchain4j-cdi/issues?q=is%3Aissue+is%3Aopen+label%3AP3) and [P4](https://github.com/langchain4j/langchain4j-cdi/issues?q=is%3Aissue+is%3Aopen+label%3AP4).

Please start with the higher priorities. PRs will be reviewed in order of priority, with bugs being a higher priority than new features.

Please note that we do not have the capacity to review PRs immediately. We ask for your patience. We are doing our best to review your PR as quickly as possible.

# Opening an issue

- Please fill in all sections of the issue template.

# Opening a draft PR

- Please open the PR as a draft initially. Once it is reviewed and approved, we will then ask you to finalize it (see section below).
- Fill in all the sections of the PR template.
- Please make it easier to review your PR:
  - Keep changes as small as possible.
  - Do not combine refactoring with changes in a single PR.
  - Avoid reformatting existing code.

# Finalizing the draft PR

- Add [documentation](https://github.com/langchain4j/langchain4j-cdi/tree/main/docs/docs) (if required).
- Add an example to the [examples repository](https://github.com/langchain4j-cdi/examples) (if required).
- [Mark a PR as ready for review](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/changing-the-stage-of-a-pull-request#marking-a-pull-request-as-ready-for-review)
