/*
 * Copyright 2014 Ludwig M Brinckmann
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapsforge.map.android.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Logger;


/*
 * The AndroidSvgBitmapStore stores rendered SVG images as bitmaps to avoid re-rendering
 * of SVG images all the time.
 * The SVG bitmaps are stored as PNG files in the applications internal directory by their
 * hash (which also encodes the size). Storage is handled asynchronously.
 *
 * There is a potential issue, which I have not yet seen in testing, that a bitmap can
 * be retrieved while it is still in process of being stored. To avoid this, it would be
 * possible to first store with a temporary name, then move to the permanent name when storage
 * is complete, but there is no renaming api for files in the internal storage.
 *
 * For developers please note that the storage is permanent, so if you introduce new icons with
 * the same name/size (=hash) you will retrieve the old, cached one unless you uninstall the
 * app first.
 */
public class AndroidSvgBitmapStore {

	private static final Logger LOGGER = Logger.getLogger(AndroidSvgBitmapStore.class.getName());
	private static final String SVG_PREFIX = "svg-";
	private static final String SVG_SUFFIX = ".png";

	private static class SvgStorer implements Runnable {
		private Bitmap bitmap;
		private int hash;

		public SvgStorer(int hash, Bitmap bitmap) {
			this.hash = hash;
			this.bitmap = bitmap;
		}

		public void run() {
			String fileName = createFileName(this.hash);
			try {
				FileOutputStream outputStream = AndroidGraphicFactory.INSTANCE.openFileOutput(fileName, Context.MODE_PRIVATE);
				if (!this.bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream)) {
					LOGGER.warning("SVG Failed to write svg bitmap " + fileName);
				}
			} catch (IllegalStateException e) {
				LOGGER.warning("SVG Failed to stream bitmap to file " + fileName);
			} catch (FileNotFoundException e) {
				LOGGER.warning("SVG Failed to create file for svg bitmap " + fileName);
			}
			return;
		}
	}

	public static void clear() {
		String[] files = AndroidGraphicFactory.INSTANCE.fileList();
		for (String file : files) {
			if (file.startsWith(SVG_PREFIX) && file.endsWith(SVG_SUFFIX)) {
				AndroidGraphicFactory.INSTANCE.deleteFile(file);
			}
		}
	}

	public static Bitmap get(int hash) {
		String fileName = createFileName(hash);
		try {
			FileInputStream inputStream = AndroidGraphicFactory.INSTANCE.openFileInput(fileName);
			return BitmapFactory.decodeStream(inputStream);
		} catch (FileNotFoundException e) {
			// ignore: file is not yet in cache
		}
		return null;
	}

	public static void put(int hash, Bitmap bitmap) {
		// perform in background
		new Thread(new SvgStorer(hash, bitmap)).start();
		return;
	}

	private static String createFileName(int hash) {
		StringBuilder sb = new StringBuilder().append(SVG_PREFIX).append(hash).append(SVG_SUFFIX);
		return sb.toString();
	}

	private AndroidSvgBitmapStore() {
		// noop
	}

}