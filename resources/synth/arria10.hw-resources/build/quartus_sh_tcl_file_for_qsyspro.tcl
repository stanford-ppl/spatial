set filesList [list]; foreach_in_collection file [get_all_assignments -type global -name IP_FILE] { append filesList [get_assignment_info $file -value]\; }; return $filesList;
