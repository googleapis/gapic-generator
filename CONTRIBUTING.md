Want to contribute? Great! First, read this page (including the small print at the end).

## Before you contribute
Before we can use your code, you must sign the
[Google Individual Contributor License Agreement]
(https://cla.developers.google.com/about/google-individual)
(CLA), which you can do online. The CLA is necessary mainly because you own the
copyright to your changes, even after your contribution becomes part of our
codebase, so we need your permission to use and distribute your code. We also
need to be sure of various other things—for instance that you'll tell us if you
know that your code infringes on other people's patents. You don't have to sign
the CLA until after you've submitted your code for review and a member has
approved it, but you must do it before we can put your code into our codebase.
Before you start working on a larger contribution, you should get in touch with
us first through the issue tracker with your idea so that we can help out and
possibly guide you. Coordinating up front makes it much easier to avoid
frustration later on.

## Code submission process

1 . Clone the repository locally

2 . Create a fork on GitHub

3 . Add your fork as a remote:

```
git remote add fork https://github.com/<GITHUB-USERNAME>/gapic-generator.git
```

4 . Make your local changes

5 . Test your local changes:

```
./gradlew googleJavaFormat build
```

6 . Pull and rebase any changes made by others to master since you started working
on your change:

```
git pull --rebase
```

7 . Also, if you committed multiple changes locally, squash them to a single
commit before submitting a PR:

```
git log
# Now find the commit ID of your first local commit, and use it to rebase
git rebase <FIRST-COMMIT-ID> -i
# Now mark all of the commits except the first as "squash"
```

We prefer a single commit as the first revision of a PR, because it is cleaner
 to review.

8 . Push your changes to your fork:

```
# Use the -f option if you have used your fork for previous PRs
git push -f fork master
```

9 . Go to your fork on GitHub and click the link to create a new pull request.

10 . Now say that your reviewers have some feedback. Make your local changes,
rerun tests (as above), then update your fork:

```
git push fork master
```

 Note: Do not squash commits after the PR has been sent! This resets state held
 by GitHub about what files each reviewer has already seen, and prevents them
 from being able to see what updates you have done.

11 . Now say that more changes have been made to master, and you need to update
your fork. Pull the master changes as a merge (not a rebase!):

```
git pull
git push fork master
```

 By using merges instead of rebases, GitHub can actually hide away the content
 of the merge commits because the reviewer doesn't really need to see them.

12 . Now say that the reviewer has approved your change with LGTM; it's time to
submit! Use the "Squash and merge" option when merging to master. We use squash
merging because the individual PR iterations are not important to understanding
the evolution of the codebase in master, and create a lot of noise in the
commit history.

## The small print
Contributions made by corporations are covered by a different agreement than
the one above, the
[Software Grant and Corporate Contributor License Agreement]
(https://cla.developers.google.com/about/google-corporate).
