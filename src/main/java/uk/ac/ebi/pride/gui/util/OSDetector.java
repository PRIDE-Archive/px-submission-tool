package uk.ac.ebi.pride.gui.util;

/**
 * Created by ilias on 02/05/14 for.detectOS project
 */
public class OSDetector
{
    private String os = System.getProperty("os.name").toLowerCase();
    private String arch = System.getProperty("os.arch");

    public String getOS()
    {
        if (os.contains("linux"))
        {
            if (arch.contains("amd64"))
            {
                return "linux64";
            }
            else
            {
                return "linux32";
            }
        }
        else if (os.contains("mac"))
        {
            return "mac";
        }
        else if (os.contains("win"))
        {
            return "windows";
        }
        else
        {
            return "unsupported";
        }
    }
//
//    public String getArch()
//    {
//        return arch;
//    }
}
