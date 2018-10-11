#! /bin/bash

export C_RESET=""
export C_RED=""
export C_GREEN=""
export C_YELLOW=""
export C_WHITE=""

function set-colors()
{
    C_RESET="$(tput sgr0)"
	C_RED="$(tput bold)$(tput setab 0)$(tput setaf 1)"
	C_GREEN="$(tput bold)$(tput setab 0)$(tput setaf 2)"
	C_YELLOW="$(tput bold)$(tput setab 0)$(tput setaf 3)"
	C_WHITE="$(tput bold)$(tput setab 0)$(tput setaf 7)"
}
