#! /bin/bash
#
# Helper function to check operating system compatibility

function check-os()
{
    local OS=""
    case $1 in
        "unix")
            OS="OS_UNIX"
            ;;
        "winnt")
            OS="OS_WINNT"
            ;;
        "win32")
            OS="OS_WIN32"
            ;;
        *)
            echo "unsupported operating system: $1" >&2; usage
            exit 1
            ;;
    esac
    preprocess-append-flags "$OS"
}
