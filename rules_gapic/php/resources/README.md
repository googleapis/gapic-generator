## How to update prebuilt PHP interpreter

There are no specific requirements on how to build the PHP interpreter, a typical build should work.

Note, the prebuilt PHP distributions are optional, if no appropriate prebuilt distribution is specified during the build, PHP will be built on the fly. The prebuilt distributions should be treated as a way to optimize the overall `bazel build` speed.

The recommended (tested) way of doing it is as follows:

1. Do the build on the oldest OS distribution you want to support (you can add prebuilt binaries for any number of operating systems and their distributions, the first one in the list which is able to complete `bin/php --version` command without errors will be picked during the `bazel build`). For example `Ubuntu 16.04 LTS`. 
2. Make sure that `libxml2-dev` and `build-essential` packages are installed on the machine:  
    ```
    sudo apt-get install \
        libxml2-dev \
        build-essential
   ```
3. Download the specific PHP distribution sources from https://www.php.net/distributions:
    ```
    curl https://www.php.net/distributions/php-7.1.30.tar.gz -o php-7.1.30.tar.gz
    ```
4. Unpack the downloaded archive:
    ```
    tar -xzpf php-7.1.30.tar.gz
    ```
5. Go to the unpacked directory:
    ```
    cd php-7.1.30
    ```
6. Run the build config:
    ```
    ./configure \
        --enable-static \
        --without-pear \
        --prefix=/tmp/php-7.1.30
    ```
    Please make sure that the `--prefix` destination folder has the same name as the root folder of your unpacked archive (i.e. the one you `cd` in step 3; it is usually `php-<version>`, e.g. `php-7.1.30`).
7. Run the build:
    ```
    make -j10
    ```
    The `-j` argument determines how many files can be compiled in parallel to speed up the build process, please use the value appropriate for your hardware (i.e. it should approximately be equal to the number of CPUs you have).
8. Install the built binaries (installation simply means that the built binaries will be copied do the directory specified as `--prefix=` during the configuration step):
    ```
    make install
    ```
9. Go to the parent directory of the directory specified as `--prefix` on configuration step:
    ```
    cd /tmp
    ```
10. Pack the relevant binaries in the archive, using `php-<version>_platform.tar.gz` name format:
    ```
    tar -zchpf php-7.1.30_linux_x86_64.tar.gz php-7.1.30/bin php-7.1.30/lib
    ```
11. Copy the created archive to the `rules_gapic/php/resources` folder in this repository and post a PR.
