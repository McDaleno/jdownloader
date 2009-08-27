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

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "http://[\\w\\.]*?youtube\\.com/(watch\\?v=[a-z-_A-Z0-9]+|view_play_list\\?p=[a-z-_A-Z0-9]+(.*?page=\\d+)?)" }, flags = { 0 })
public class TbCm extends PluginForDecrypt {

    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    HashMap<ConversionMode, ArrayList<Info>> possibleconverts = null;

    public TbCm(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 100);
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    private boolean login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.getPage("http://www.youtube.com/");

        br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252F&hl=en_US&ltmpl=sso");
        br.setFollowRedirects(false);
        String cook = br.getCookie("http://www.google.com", "GALX");
        if (cook == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.postPage("https://www.google.com/accounts/ServiceLoginAuth?service=youtube", "ltmpl=sso&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252F&service=youtube&uilel=3&ltmpl=sso&hl=en_US&ltmpl=sso&GALX=" + cook + "&Email=" + Encoding.urlEncode(account.getUser()) + "&Passwd=" + Encoding.urlEncode(account.getPass()) + "&PersistentCookie=yes&rmShown=1&signIn=Sign+in&asts=");
        if (br.getRedirectLocation() == null) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        br.setFollowRedirects(true);
        br.getPage(br.getRedirectLocation());
        if (br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        br.getPage("http://www.youtube.com/index?hl=en");
        return true;
    }

    private void addVideosCurrentPage(ArrayList<DownloadLink> links) {
        String[] videos = br.getRegex("id=\"video-url-.*?href=\"(/watch\\?v=.*?)&").getColumn(0);
        for (String video : videos) {
            links.add(this.createDownloadlink("http://www.youtube.com" + video));
        }
    }

    static class Info {
        public String link;
        public long size;
        public String desc;
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        possibleconverts = new HashMap<ConversionMode, ArrayList<Info>>();

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");
        if (parameter.contains("view_play_list")) {
            br.getPage(parameter);
            addVideosCurrentPage(decryptedLinks);
            if (!parameter.contains("page=")) {
                String[] pages = br.getRegex("<a href=(\"|')(http://www.youtube.com/view_play_list\\?p=.*?page=\\d+)(\"|')").getColumn(1);
                for (int i = 0; i < pages.length - 1; i++) {
                    br.getPage(pages[i]);
                    addVideosCurrentPage(decryptedLinks);
                }
            }
        } else {
            boolean prem = false;
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
            if (accounts != null && accounts.size() != 0) prem = login(accounts.get(0));
            br.setFollowRedirects(true);
            try {
                if (StreamingShareLink.matcher(parameter).matches()) {
                    // StreamingShareLink

                    String[] info = new Regex(parameter, StreamingShareLink).getMatches()[0];

                    for (String debug : info) {
                        logger.info(debug);
                    }
                    DownloadLink thislink = createDownloadlink(info[1]);
                    thislink.setBrowserUrl(info[2]);
                    thislink.setFinalFileName(info[0]);
                    thislink.setSourcePluginComment("Convert to " + (ConversionMode.valueOf(info[3])).getText());
                    thislink.setProperty("convertto", info[3]);

                    decryptedLinks.add(thislink);
                    return decryptedLinks;
                }

                br.getPage(parameter);
                if (br.containsHTML("verify_age") && prem) {
                    String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
                    String url = br.getURL();
                    LinkedHashMap<String, String> p = Request.parseQuery(url);
                    String next = p.get("next_url");
                    Form form = new Form();
                    form.setAction(url);
                    form.setMethod(MethodType.POST);
                    form.put("next_url", "%2F" + next.substring(1));
                    form.put("action_confirm", "Confirm+Birth+Date");
                    form.put("session_token", Encoding.urlEncode(session_token));
                    br.submitForm(form);
                    if (br.getCookie("http://www.youtube.com", "is_adult") == null) return null;
                } else if (br.containsHTML("verify_age")) throw new DecrypterException(DecrypterException.ACCOUNT);
                String video_id = "";
                String t = "";
                String match = br.getRegex(patternswfArgs).getMatch(0);
                if (match == null) throw new DecrypterException("Video seems to be offline");

                /* DownloadUrl holen */
                String[] lineSub = match.split(",|:");
                for (int i = 0; i < lineSub.length; i++) {
                    String s = lineSub[i];
                    if (s.indexOf(VIDEO_ID) > -1) {
                        video_id = clean(lineSub[i + 1]);
                    }
                    if (s.indexOf(T) > -1) {
                        t = clean(lineSub[i + 1]);
                    }
                }
                String link = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;
                String name = Encoding.htmlDecode(br.getRegex(YT_FILENAME).getMatch(0).trim());
                /* Konvertierungsmöglichkeiten adden */
                boolean tryall = false;
                while (true) {
                    if (tryall || (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOFLV) || ConvertDialog.getKeeped().contains(ConversionMode.AUDIOMP3) || ConvertDialog.getKeeped().contains(ConversionMode.AUDIOMP3_AND_VIDEOFLV))) {
                        if (br.openGetConnection(link).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOFLV, link, br.getHttpConnection().getLongContentLength(), "");
                            addtopos(ConversionMode.AUDIOMP3, link, br.getHttpConnection().getLongContentLength(), "");
                            addtopos(ConversionMode.AUDIOMP3_AND_VIDEOFLV, link, br.getHttpConnection().getLongContentLength(), "");
                            br.getHttpConnection().disconnect();
                            if ((ConvertDialog.getKeeped().contains(ConversionMode.VIDEOFLV) || ConvertDialog.getKeeped().contains(ConversionMode.AUDIOMP3) || ConvertDialog.getKeeped().contains(ConversionMode.AUDIOMP3_AND_VIDEOFLV))) break;
                        }
                    }
                    if (tryall || ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4)) {
                        if (br.openGetConnection(link + "&fmt=18").getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, link + "&fmt=18", br.getHttpConnection().getLongContentLength(), "(18)");
                            br.getHttpConnection().disconnect();
                        }
                        if (br.openGetConnection(link + "&fmt=22").getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, link + "&fmt=22", br.getHttpConnection().getLongContentLength(), "(22)");
                            br.getHttpConnection().disconnect();
                        }
                        if (br.openGetConnection(link + "&fmt=35").getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, link + "&fmt=35", br.getHttpConnection().getLongContentLength(), "(35)");
                            br.getHttpConnection().disconnect();
                        }
                        if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4)) break;
                    }
                    if (tryall) {
                        if (br.openGetConnection(link + "&fmt=13").getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEO3GP, link + "&fmt=13", br.getHttpConnection().getLongContentLength(), "(13)");
                            br.getHttpConnection().disconnect();
                        }
                    }
                    if (tryall == true) break;
                    tryall = true;
                }
                ConversionMode convertTo = Plugin.showDisplayDialog(new ArrayList<ConversionMode>(possibleconverts.keySet()), name, param);

                for (Info info : possibleconverts.get(convertTo)) {
                    DownloadLink thislink = createDownloadlink(info.link);
                    thislink.setBrowserUrl(parameter);
                    thislink.setFinalFileName(name + info.desc + ".tmp");
                    thislink.setSourcePluginComment("Convert to " + convertTo.getText());
                    thislink.setProperty("size", new Long(info.size));
                    thislink.setProperty("name", name + info.desc + ".tmp");
                    thislink.setProperty("convertto", convertTo.name());
                    decryptedLinks.add(thislink);
                }
            } catch (IOException e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                return null;
            }
        }
        return decryptedLinks;
    }

    private void addtopos(ConversionMode mode, String link, long size, String desc) {
        if (possibleconverts.containsKey(mode)) {
            Info tmp = new Info();
            tmp.link = link;
            tmp.size = size;
            tmp.desc = desc;
            possibleconverts.get(mode).add(tmp);
        } else {
            ArrayList<Info> tmp2 = new ArrayList<Info>();
            Info tmp = new Info();
            tmp.link = link;
            tmp.size = size;
            tmp.desc = desc;
            tmp2.add(tmp);
            possibleconverts.put(mode, tmp2);
        }
    }

    // @Override

}
