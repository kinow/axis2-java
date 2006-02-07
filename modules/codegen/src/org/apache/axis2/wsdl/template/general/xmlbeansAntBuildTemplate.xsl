<xsl:stylesheet version="1.0" xmlns:xalan="http://xml.apache.org/xslt"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" xalan:indent-amount="4"/>
    <xsl:template match="/ant">
        <xsl:variable name="package"><xsl:value-of select="@package"/></xsl:variable>

        <project basedir="." default="jar.all">
            <xsl:comment>Auto generated ant build file</xsl:comment>
            <property environment="env"/>
            <property name="axis2.home">
                <xsl:attribute name="value">${env.AXIS2_HOME}</xsl:attribute>
            </property>
            <property name="project.base.dir">
                <xsl:attribute name="value">.</xsl:attribute>
            </property>
            <property name="maven.class.path">
                <xsl:attribute name="value"></xsl:attribute>
            </property>
            <property name="name">
                <xsl:attribute name="value"><xsl:value-of select="@servicename"/></xsl:attribute>
            </property>
            <property name="src">
                <xsl:attribute name="value">${project.base.dir}/src</xsl:attribute>
            </property>
            <property name="test">
                <xsl:attribute name="value">${project.base.dir}/test</xsl:attribute>
            </property>
            <property name="build">
                <xsl:attribute name="value">${project.base.dir}/build</xsl:attribute>
            </property>
            <property name="classes">
                <xsl:attribute name="value">${build}/classes</xsl:attribute>
            </property>
            <property name="lib">
                <xsl:attribute name="value">${build}/lib</xsl:attribute>
            </property>
            <property name="resources">
                <xsl:attribute name="value">${project.base.dir}/resources</xsl:attribute>
            </property>

            <property name="xbeans.packaged.jar.name" value="XBeans-packaged.jar"></property>

            <property name="jars.ok" value=""></property>

            <path id="axis2.class.path">
                <pathelement>
                    <xsl:attribute name="path">${java.class.path}</xsl:attribute>
                </pathelement>
                <pathelement>
                    <xsl:attribute name="path">${maven.class.path}</xsl:attribute>
                </pathelement>
                <fileset>
                    <xsl:attribute name="dir">${axis2.home}</xsl:attribute>
                    <include>
                        <xsl:attribute name="name">lib/*.jar</xsl:attribute>
                    </include>
                </fileset>
            </path>

            <target name="init">
                <mkdir>
                    <xsl:attribute name="dir">${build}</xsl:attribute>
                </mkdir>
                <mkdir>
                    <xsl:attribute name="dir">${classes}</xsl:attribute>
                </mkdir>
                <mkdir>
                    <xsl:attribute name="dir">${lib}</xsl:attribute>
                </mkdir>
                <xsl:if test="not(@testOmit)">
                    <mkdir>
                        <xsl:attribute name="dir">${test}</xsl:attribute>
                    </mkdir>
                </xsl:if>

            </target>

            <target name="jar.xbeans">
                <!-- jar the  XMLbeans stuff to the lib folder-->
                <jar>
                    <xsl:attribute name="basedir">${resources}</xsl:attribute>
                    <xsl:attribute name="destfile">${lib}/${xbeans.packaged.jar.name}</xsl:attribute>
                    <xsl:attribute name="excludes">**/services.xml</xsl:attribute>
                </jar>
            </target>

            <target name="pre.compile.test" depends="init, jar.xbeans">
                <xsl:comment>Test the classpath for the availability of necesary classes</xsl:comment>
                <available classname="org.apache.xmlbeans.XmlObject" property="xbeans.available" classpathref="axis2.class.path"/>
                <available classname="javax.xml.stream.XMLStreamReader" property="stax.available" classpathref="axis2.class.path"/>
                <available classname="org.apache.axis2.engine.AxisEngine" property="axis2.available" classpathref="axis2.class.path"/>
                <condition property="jars.ok" >
                    <and>
                        <isset property="xbeans.available"/>
                        <isset property="stax.available"/>
                        <isset property="axis2.available"/>
                    </and>
                </condition>

                <xsl:comment>Print out the availabilities</xsl:comment>
                <echo>
                    <xsl:attribute name="message">XmlBeans Availability = ${xbeans.available}</xsl:attribute>
                </echo>
                <echo>
                    <xsl:attribute name="message">Stax Availability= ${stax.available}</xsl:attribute>
                </echo>
                <echo>
                    <xsl:attribute name="message">Axis2 Availability= ${axis2.available}</xsl:attribute>
                </echo>

            </target>

            <target name="compile.src" depends="pre.compile.test" >
                <xsl:attribute name="if">jars.ok</xsl:attribute>
                <javac debug="on">
                    <xsl:attribute name="destdir">${classes}</xsl:attribute>
                    <xsl:attribute name="srcdir">${src}</xsl:attribute>
                    <classpath>
                        <xsl:attribute name="location">${lib}/${xbeans.packaged.jar.name}</xsl:attribute>
                    </classpath>
                    <classpath>
                        <xsl:attribute name="refid">axis2.class.path</xsl:attribute>
                    </classpath>
                </javac>
            </target>


            <target name="compile.test" depends="compile.src">
                <xsl:attribute name="if">jars.ok</xsl:attribute>
                <javac debug="on">
                    <xsl:attribute name="destdir">${classes}</xsl:attribute>

                    <src>
                        <xsl:attribute name="path">${test}</xsl:attribute>
                    </src>

                    <classpath>
                        <xsl:attribute name="location">${lib}/${xbeans.packaged.jar.name}</xsl:attribute>
                    </classpath>
                    <classpath>
                        <xsl:attribute name="refid">axis2.class.path</xsl:attribute>
                    </classpath>

                    <!-- todo -->

                </javac>
            </target>

            <target name="echo.classpath.problem" depends="pre.compile.test">
                <xsl:attribute name="unless">jars.ok</xsl:attribute>
                <echo message="The class path is not set right!
                               Please make sure the following classes are in the classpath
                               1. XmlBeans
                               2. Stax
                               3. Axis2
                "></echo>
            </target>
            <target name="jar.all" depends="jar.server, jar.client">
            </target>
            <target name="jar.server" depends="compile.src,echo.classpath.problem">
                <xsl:attribute name="if">jars.ok</xsl:attribute>
                <copy>
                    <xsl:attribute name="toDir">${classes}/META-INF</xsl:attribute>
                    <fileset>
                        <xsl:attribute name="dir">${resources}</xsl:attribute>
                        <include><xsl:attribute name="name">*.xml</xsl:attribute></include>
                        <include><xsl:attribute name="name">*.wsdl</xsl:attribute></include>
                        <exclude><xsl:attribute name="name">**/schemaorg_apache_xmlbean/**</xsl:attribute></exclude>
                    </fileset>
                </copy>
                <copy>
                    <xsl:attribute name="file">${lib}/${xbeans.packaged.jar.name}</xsl:attribute>
                    <xsl:attribute name="toDir">${classes}/lib</xsl:attribute>
                </copy>
                <jar>
                    <xsl:attribute name="destfile">${lib}/${name}.aar</xsl:attribute>
                    <fileset>
                        <xsl:attribute name="excludes">**/Test.class</xsl:attribute>
                        <xsl:attribute name="dir">${classes}</xsl:attribute>
                    </fileset>
                </jar>
            </target>



            <target  name="jar.client" if="jars.ok">
                <!--set the correct depends target-->
                <xsl:choose>
                    <xsl:when test="@testOmit">
                        <xsl:attribute name="depends">compile.src</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="depends">compile.test</xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>


                <jar>
                    <xsl:attribute name="destfile">${lib}/${name}-test-client.jar</xsl:attribute>
                    <fileset>
                        <xsl:attribute name="dir">${classes}</xsl:attribute>
                        <exclude><xsl:attribute name="name">**/META-INF/*.*</xsl:attribute></exclude>
                        <exclude><xsl:attribute name="name">**/lib/*.*</xsl:attribute></exclude>
                        <exclude><xsl:attribute name="name">**/*MessageReceiver.class</xsl:attribute></exclude>
                        <exclude><xsl:attribute name="name">**/*Skeleton.class</xsl:attribute></exclude>

                    </fileset>
                    <fileset>
                        <xsl:attribute name="dir">${resources}</xsl:attribute>
                        <exclude><xsl:attribute name="name">**/*.wsdl</xsl:attribute></exclude>
                        <exclude><xsl:attribute name="name">**/*.xml</xsl:attribute></exclude>
                    </fileset>
                </jar>
            </target>






            <target depends="jar.server" name="make.repo" if="jars.ok">
                <mkdir>
                    <xsl:attribute name="dir">${build}/repo/</xsl:attribute>
                </mkdir>
                <mkdir>
                    <xsl:attribute name="dir">${build}/repo/services</xsl:attribute>
                </mkdir>
                <copy>
                    <xsl:attribute name="file">${build}/lib/${name}.aar</xsl:attribute>
                    <xsl:attribute name="toDir">${build}/repo/services/</xsl:attribute>
                </copy>
            </target>
            <target depends="make.repo" name="start.server" if="jars.ok">
                <java classname="org.apache.axis2.transport.http.SimpleHTTPServer" fork="true">
                    <arg>
                        <xsl:attribute name="value">${build}/repo</xsl:attribute>
                    </arg>
                    <classpath>
                        <xsl:attribute name="refid">axis2.class.path</xsl:attribute>
                    </classpath>
                </java>
            </target>
            <target if="jars.ok" name="run.test" depends="jar.client">
                <path id="test.class.path">
                    <pathelement>
                        <xsl:attribute name="location">${lib}/${name}-test-client.jar</xsl:attribute>
                    </pathelement>
                    <path>
                        <xsl:attribute name="refid">axis2.class.path</xsl:attribute>
                    </path>
                </path>
                <mkdir>
                    <xsl:attribute name="dir">${build}/test-reports/</xsl:attribute>
                </mkdir>
                <junit printsummary="yes" haltonfailure="yes">
                    <classpath>
                        <xsl:attribute name="refid">test.class.path</xsl:attribute>
                    </classpath>
                    <formatter type="plain"/>
                    <batchtest fork="yes">
                        <xsl:attribute name="toDir">${build}/test-reports/</xsl:attribute>
                        <fileset>
                            <xsl:attribute name="dir">${test}</xsl:attribute>
                            <include>
                                <xsl:attribute name="name">**/*Test*.java</xsl:attribute>
                            </include>
                        </fileset>
                    </batchtest>
                </junit>
            </target>
            <target name="clean">
                <delete>
                    <xsl:attribute name="dir">${build}</xsl:attribute>
                </delete>
            </target>
        </project>
    </xsl:template>
</xsl:stylesheet>
