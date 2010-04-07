//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tab.net.ua" }, urls = { "http://[\\w\\.]*?tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.\\d+/" }, flags = { 0 })
public class TabNetUa extends PluginForHost {

    public TabNetUa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tab.net.ua/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("<span style=\"font-size:21px; color:#091E35; font-family:'Trebuchet MS';\">(.*?)</span><br>").getMatch(0);
        if (filename == null) filename = br.getRegex("<input type=\"text\" id=\"re\" value=\"Re:(.*?)\"").getMatch(0);
        String filesize = br.getRegex(">Розмір файлу:(.*?), завантажено").getMatch(0);
        if (filename == null || filename.equals("") || (filesize != null && filesize.equals(" 0 байт"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) {
            if (filesize.contains("Gб") || filesize.contains("Гб"))
                filesize = filesize.replaceAll("(Gб|Гб)", "GB");
            else if (filesize.contains("")) filesize = filesize.replace("Мб", "MB");
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        for (int i = 0; i <= 3; i++) {
            String captchaUrl = br.getRegex("Введіть число, вказане на картинці:<br>.*?<img src=\"(http://tab\\.net\\.ua/.*?)\"").getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex("\"(http://tab\\.net\\.ua/tools/antispam\\.php\\?id=[a-z0-9]+\\&r=\\d+)\"").getMatch(0);
            if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(br.getURL(), "antispam=" + code);
            if (br.containsHTML("(>Не співпадає з числом на картинці<|antispam.php?)")) continue;
            break;
        }
        if (br.containsHTML("Вы ввели неправильный код проверки")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("; background-repeat:no-repeat;\">.*?<a href=\"(http://.*?)\" class=\"file_download\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://d\\d+\\.tab\\.net\\.ua:\\d+/\\d+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>404 Not Found</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}