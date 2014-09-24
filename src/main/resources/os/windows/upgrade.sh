
echo "starting download script"
echo "Args to shell:" $*

# ARG 1: Path to lsz executable.
# ARG 2: sysimage File to download
# ARG 3: COM port to use.

#path contains \ need to change all to /
path_to_exe=$1
fixed_path=${path_to_exe//\\/\/}

com_port_arg=$3
com_port_id=${com_port_arg/COM/}
#echo "COM PORT" $com_port_id
tty_port_id=/dev/ttyS$((com_port_id-1))
#echo "Converted COM Port" $com_port_arg "to tty port" $tty_port_id
echo $tty_port_id

echo "NO OF ARGS = $#"

if [ "$#" -eq "3" ]
then
host_file_name=$2
echo "download firmware onto target"
"$fixed_path/lsz.exe" --escape --binary --overwrite "$fixed_path/$host_file_name" <> $tty_port_id 1>&0
elif [ "$#" -gt "3" ]
then
echo "execute remote command"
remote_cmd=$4
remote_arg1=$5
remote_arg2=$6
remote_arg3=$7
"$fixed_path/lsz.exe" --escape -v -c "$remote_cmd $remote_arg1 $remote_arg2 $remote_arg3" <> $tty_port_id 1>&0
#ls -al
fi

#echo "execute..."

