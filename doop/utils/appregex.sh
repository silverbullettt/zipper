#! /bin/bash
#
# Helper function to create a regex of the application classes


function application-regex()
{
    local jar="$1"
    shift
    
    if flag-isset interactive; then
        # Dacapo benchmarks defaults
        case "$(basename $jar .jar)" in
            "antlr") defapps="dacapo.antlr.*:antlr.**";;
            "bloat") defapps="dacapo.bloat.*:EDU.**";;
            "chart") defapps="dacapo.chart.*:org.jfree.chart.**:org.jfree.data.**";;
            "eclipse") defapps="dacapo.eclipse.*:org.eclipse.**:org.osgi.**";;
            "fop") defapps="dacapo.fop.*:org.apache.fop.**";;
            "hsqldb") defapps="dacapo.hsqldb.*:org.hsqldb.**";;
            "jython") defapps="dacapo.jython.*:com.ziclix.**:javatests.*:jxxload_help.*:org.python.**:org.apache.oro.**";;
            "luindex") defapps="dacapo.luindex.*:dacapo.lusearch.*:org.apache.lucene.**";;
            "lusearch") defapps="dacapo.luindex.*:dacapo.lusearch.*:org.apache.lucene.**";;
            "pmd") defapps="dacapo.pmd.*:net.sourceforge.pmd.**";;
            "xalan") defapps="dacapo.xalan.*:org.apache.xalan.**:org.apache.xml.dtm.**:org.apache.xml.utils.**:org.apache.xpath.**:org.w3c.dom.xpath.*";;
            *) defapps=`${DOOP_HOME}/bin/application-regex "$jar" "$@"`;;
        esac

        # Read application regex from user
        read -e -i "$defapps" -p "Enter regexp for application classes: " apps

        local apps="${apps:-$defapps}"
    else
        # Automatically determine regex for all the classes of the input jar
        # and nothing else
        local apps=`${DOOP_HOME}/bin/application-regex "$jar" "$@"`
    fi
    
    echo $apps
}
