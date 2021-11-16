package com.alipay.sofa.ark.springboot.loader;

import com.alipay.sofa.ark.common.util.ClassLoaderUtils;
import com.alipay.sofa.ark.loader.EmbedClassPathArchive;
import com.alipay.sofa.ark.spi.archive.BizArchive;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A faster exploded archive launcher
 *
 * @author bingjie.lbj
 */
public class ExplodedLauncherTest extends TestCase {

    @Test
    public void testSpringBootFatJar() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("sample-springboot-fat-biz.jar");
        URL[] agentUrl = ClassLoaderUtils.getAgentClassPath();
        Assert.assertEquals(1, agentUrl.length);

        List<URL> urls = new ArrayList<>();
        JarFileArchive jarFileArchive = new JarFileArchive(new File(url.getFile()));
        List<Archive> archives = jarFileArchive.getNestedArchives(this::isNestedArchive);
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        urls.addAll(Arrays.asList(agentUrl));

        EmbedClassPathArchive classPathArchive = new EmbedClassPathArchive(
                this.getClass().getCanonicalName(), null, urls.toArray(new URL[] {}));
        List<BizArchive> bizArchives = classPathArchive.getBizArchives();
        Assert.assertEquals(0, bizArchives.size());
        Assert.assertNotNull(classPathArchive.getContainerArchive());
        Assert.assertEquals(1, classPathArchive.getPluginArchives().size());
        Assert.assertEquals(archives.size() + 1, urls.size());
        Assert.assertEquals(3, classPathArchive.getConfClasspath().size());
        URLClassLoader classLoader = new URLClassLoader(classPathArchive.getContainerArchive().getUrls());
        try {
            Class clazz = classLoader.loadClass("com.alipay.sofa.ark.bootstrap.ArkLauncher");
            Assert.assertTrue(clazz != null);
        } catch (Exception e){
            Assert.assertTrue("loadClass class failed ",false);
        }
    }

    public void testGetContainerArchive() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL springbootFatJar = cl.getResource("sample-springboot-fat-biz.jar");
        JarFileArchive jarFileArchive = new JarFileArchive(new File(springbootFatJar.getFile()));
        List<Archive> archives = jarFileArchive.getNestedArchives(this::isNestedArchive);
        List<URL> urls = new ArrayList<>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }

        EmbedClassPathArchive archive = new EmbedClassPathArchive(
                "com.alipay.sofa.ark.sample.springbootdemo.SpringbootDemoApplication", "main",
                urls.toArray(new URL[] {}));
        assertTrue(archive.getContainerArchive().getUrls().length != 0);
        assertTrue(archive.getConfClasspath().size() != 0);
        assertTrue(archive.getBizArchives().size() == 0);
        assertTrue(archive.getPluginArchives().size() == 1);

        URLClassLoader classLoader = new URLClassLoader(archive.getContainerArchive().getUrls());
        try {
            Class clazz = classLoader.loadClass("com.alipay.sofa.ark.container.ArkContainer");
            assertTrue(clazz != null);
        } catch (Exception e) {
            assertTrue("loadClass class failed ", false);
        }
    }

    protected boolean isNestedArchive(Archive.Entry entry) {
        return entry.isDirectory() ? entry.getName().equals("BOOT-INF/classes/") : entry.getName()
                .startsWith("BOOT-INF/lib/");
    }
}