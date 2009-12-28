$good = 0;
$bad = 0;
while (true) {
    $code = system("java -classpath ../../build/lib/janino.jar:.  MultiThreadedIssueTest > /dev/null 2> /dev/null");
    if ($code == 0) {
        ++$good;
    } else {
        ++$bad;
    }
    print STDERR "\rGood= ",$good," Bad= ", $bad, "          ";
}
