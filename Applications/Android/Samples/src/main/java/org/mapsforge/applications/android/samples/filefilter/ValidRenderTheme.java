package org.mapsforge.applications.android.samples.filefilter;

/**
 * Created by vkandroidstudioadm on 13.02.14.
 */

import org.mapsforge.map.reader.header.FileOpenResult;

import java.io.File;

/**
 * Accepts all valid render theme XML files.
 */
public final class ValidRenderTheme implements ValidFileFilter {
    private FileOpenResult fileOpenResult;

    @Override
    public boolean accept(File file) {
        this.fileOpenResult = FileOpenResult.SUCCESS;
        //return true;
        //InputStream inputStream = null;

        /*try {
            inputStream = new FileInputStream(file);
            RenderThemeHandler renderThemeHandler = new RenderThemeHandler(this.graphicFactory,this.displayModel,this.relativePathPrefix);
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            xmlReader.setContentHandler(renderThemeHandler);
            xmlReader.parse(new InputSource(inputStream));
            this.fileOpenResult = FileOpenResult.SUCCESS;
        } catch (ParserConfigurationException e) {
            this.fileOpenResult = new FileOpenResult(e.getMessage());
        } catch (SAXException e) {
            this.fileOpenResult = new FileOpenResult(e.getMessage());
        } catch (IOException e) {
            this.fileOpenResult = new FileOpenResult(e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                this.fileOpenResult = new FileOpenResult(e.getMessage());
            }
        }*/

        return this.fileOpenResult.isSuccess();
    }

    @Override
    public FileOpenResult getFileOpenResult() {
        return this.fileOpenResult;
    }

}
