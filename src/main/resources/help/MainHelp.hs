<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
        PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
        "http://java.sun.com/products/javahelp/helpset_1_0.dtd">

<helpset version="1.0">
    <!-- maps -->
    <maps>
        <homeID>top</homeID>
        <mapref location="Map.jhm"/>
    </maps>

    <!-- views -->
    <view>
        <name>TOC</name>
        <label>Table Of Contents</label>
        <type>javax.help.TOCView</type>
        <image>toc_icon</image>
        <data>MainHelpTOC.xml</data>
    </view>

    <presentation default="true">
        <name>main window</name>
        <size width="1200" height="600"/>
        <location x="200" y="200"/>
        <title>ProteomeXchange Submission Tool - Help</title>
        <image>help_window_icon</image>
        <toolbar>
            <helpaction image="previous_page_icon">javax.help.BackAction</helpaction>
            <helpaction image="next_page_icon">javax.help.ForwardAction</helpaction>
            <helpaction image="home_page_icon">javax.help.HomeAction</helpaction>
            <helpaction image="print_icon">javax.help.PrintAction</helpaction>
            <helpaction image="print_setting_icon">javax.help.PrintSetupAction</helpaction>
        </toolbar>
    </presentation>
</helpset>
