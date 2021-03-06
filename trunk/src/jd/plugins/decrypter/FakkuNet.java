//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fakku.net" }, urls = { "http://(www\\.)?fakku\\.net/(viewmanga\\.php\\?id=\\d+|redirect\\.php\\?type=m\\&id=[A-Za-z0-9]+)" }, flags = { 0 })
public class FakkuNet extends PluginForDecrypt {

    public FakkuNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (new Regex(parameter, "http://(www\\.)?fakku\\.net/viewmanga\\.php\\?id=\\d+").matches()) {
            br.getPage(parameter + "&mode=download");
            String fpName = br.getRegex("<title>(.*?) \\| Download</title>").getMatch(0);
            String[] links = br.getRegex("\"(/redirect\\.php\\?type=m\\&id=[A-Za-z0-9]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                final String finallink = decryptSingleLink("http://www.fakku.net" + singleLink);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }
            }
        } else {
            final String finallink = decryptSingleLink(parameter);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

    private String decryptSingleLink(String singleLink) throws IOException {
        br.getPage(singleLink);
        String finallink = br.getRedirectLocation();
        if (finallink != null) finallink = "directhttp://" + finallink;
        return finallink;
    }
}
