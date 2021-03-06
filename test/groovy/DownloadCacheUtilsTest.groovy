import com.sap.piper.DownloadCacheUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import util.BasePiperTest
import util.JenkinsFileExistsRule
import util.JenkinsWriteFileRule
import util.Rules

import static org.junit.Assert.*

class DownloadCacheUtilsTest extends BasePiperTest {
    private JenkinsFileExistsRule fileExistsRule = new JenkinsFileExistsRule(this, [])

    @Rule
    public RuleChain ruleChain = Rules
        .getCommonRules(this)
        .around(fileExistsRule)
        .around(new JenkinsWriteFileRule(this))

    @Before
    void init() {
        helper.registerAllowedMethod("libraryResource", [String.class], { path ->
            File resource = new File(new File('resources'), path)
            if (resource.exists()) {
                return resource.getText()
            }
            return ''
        })
        helper.registerAllowedMethod('node', [String.class, Closure.class]) { s, body ->
            body()
        }
    }

    @Test
    void 'isEnabled should return true if dl cache is enabled'() {
        nullScript.env.DL_CACHE_HOSTNAME = 'cx-downloadcache'
        nullScript.env.DL_CACHE_NETWORK = 'cx-network'
        boolean actual = DownloadCacheUtils.isEnabled(nullScript)

        assertTrue(actual)
    }

    @Test
    void 'getDockerOptions should return docker network if configured'() {
        nullScript.env.DL_CACHE_NETWORK = 'cx-network'
        String expected = '--network=cx-network'
        String actual = DownloadCacheUtils.getDockerOptions(nullScript)

        assertEquals(expected, actual)
    }

    @Test
    void 'getGlobalMavenSettingsForDownloadCache should write file'() {
        nullScript.env.DL_CACHE_HOSTNAME = 'cx-downloadcache'
        boolean writeFileExecuted = false

        helper.registerAllowedMethod('writeFile', [Map.class]) { Map m ->
            writeFileExecuted = true
        }
        String expected = '.pipeline/global_settings.xml'
        String actual = DownloadCacheUtils.getGlobalMavenSettingsForDownloadCache(nullScript)

        assertEquals(expected, actual)
        assertTrue(writeFileExecuted)
    }

    @Test
    void 'getGlobalMavenSettingsForDownloadCache should return filePath if file already exists'() {
        fileExistsRule.registerExistingFile('.pipeline/global_settings.xml')
        nullScript.env.DL_CACHE_HOSTNAME = 'cx-downloadcache'
        boolean writeFileExecuted = false

        helper.registerAllowedMethod('writeFile', [Map.class]) { Map m ->
            writeFileExecuted = true
        }

        String expected = '.pipeline/global_settings.xml'
        String actual = DownloadCacheUtils.getGlobalMavenSettingsForDownloadCache(nullScript)
        assertFalse(writeFileExecuted)
        assertEquals(expected, actual)
    }

    @Test
    void 'getGlobalMavenSettingsForDownloadCache should return empty string if dl cache not active'() {
        String expected = ''
        String actual = DownloadCacheUtils.getGlobalMavenSettingsForDownloadCache(nullScript)

        assertEquals(expected, actual)
    }

    @Test
    void 'injectDownloadCacheInMavenParameters should not change the parameters if dl cache not active'() {
        Map newParameters = DownloadCacheUtils.injectDownloadCacheInMavenParameters(nullScript, [:])
        assertTrue(newParameters.isEmpty())
    }

    @Test
    void 'injectDownloadCacheInMavenParameters should set docker options and global settings'() {
        nullScript.env.DL_CACHE_HOSTNAME = 'cx-downloadcache'
        nullScript.env.DL_CACHE_NETWORK = 'cx-network'

        Map expected = [
            dockerOptions: ['--network=cx-network'],
            globalSettingsFile: '.pipeline/global_settings.xml'
        ]

        Map actual = DownloadCacheUtils.injectDownloadCacheInMavenParameters(nullScript, [:])

        assertEquals(expected, actual)
    }

    @Test
    void 'injectDownloadCacheInMavenParameters should append docker options'() {
        nullScript.env.DL_CACHE_HOSTNAME = 'cx-downloadcache'
        nullScript.env.DL_CACHE_NETWORK = 'cx-network'

        List expectedDockerOptions = ['--test', '--network=cx-network']



        Map actual = DownloadCacheUtils.injectDownloadCacheInMavenParameters(nullScript, [dockerOptions: '--test'])

        assertEquals(expectedDockerOptions, actual.dockerOptions)
    }
}
