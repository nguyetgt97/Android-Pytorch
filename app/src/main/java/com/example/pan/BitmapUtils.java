package com.example.pan;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class BitmapUtils {
	public static final String TAG = "BitmapUtils";
	public static int estimateSampleSize(String filePath,
                                         int destWidth, int destHeight) {
		return estimateSampleSize(filePath, destWidth, destHeight, 0);
	}

	public static Bitmap blurBitmapUseRenderScript(Context context, Bitmap inputBitmap, float scale, float radius) {
		ScriptIntrinsicBlur mBlurbitmapscript = null;
		RenderScript renderScript = null;
		if (inputBitmap == null || scale == 0) {
			return null;
		}
		//inputBitmap = darkenBitMap(inputBitmap);
		Bitmap inputBitmapForBlur = Bitmap.createScaledBitmap(inputBitmap, (int) (inputBitmap.getWidth() / scale),
				(int) (inputBitmap.getHeight() / scale), true);
		if (renderScript == null) {
			renderScript = RenderScript.create(context);
		}
		if (mBlurbitmapscript == null) {
			mBlurbitmapscript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
		}
		Allocation alloc = Allocation.createFromBitmap(renderScript, inputBitmapForBlur);
		mBlurbitmapscript.setRadius(radius);
		mBlurbitmapscript.setInput(alloc);
		Bitmap scaledBlurResult = Bitmap.createBitmap(inputBitmapForBlur.getWidth(), inputBitmapForBlur.getHeight(),
				inputBitmapForBlur.getConfig());
		Allocation outAlloc = Allocation.createFromBitmap(renderScript, scaledBlurResult);
		mBlurbitmapscript.forEach(outAlloc);
		outAlloc.copyTo(scaledBlurResult);
		Bitmap blurResult = Bitmap
				.createScaledBitmap(scaledBlurResult, inputBitmap.getWidth(), inputBitmap.getHeight(), true);
		scaledBlurResult.recycle();
		alloc.destroy();
		outAlloc.destroy();
		return blurResult;
	}


	public static Bitmap USM(Context context, Bitmap input, float scale, float amount) {
		int w = input.getWidth();
		int h = input.getHeight();
		int[] mIntValues = new int[w * h];
		int[] mBlurIntValues = new int[w * h];
		int[] mOutIntValues = new int[w * h];
		input.getPixels(mIntValues, 0, w, 0, 0, w, h);
		//Bitmap blur = blurBitmapUseRenderScript(context, input, 1.2f, 3.4f);
		Bitmap blur = blurBitmapUseRenderScript(context, input, scale, scale*1.5f);
		blur.getPixels(mBlurIntValues, 0, w, 0, 0, w, h);
		int orgRed = 0, orgGreen = 0, orgBlue = 0;
		int blurredRed = 0, blurredGreen = 0, blurredBlue = 0;
		//USM filter parameters
		int threshold = 6;
		int usmPixel = 0;
		int alpha = 0xFF000000; //transperency is not considered and always zero
		Bitmap output = Bitmap.createBitmap(w, h, input.getConfig());
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int origPixel = mIntValues[y * w + x];
				int blurredPixel = mBlurIntValues[y * w + x];
				orgRed = ((origPixel >> 16) & 0xff);
				orgGreen = ((origPixel >> 8) & 0xff);
				orgBlue = (origPixel & 0xff);
				blurredRed = ((blurredPixel >> 16) & 0xff);
				blurredGreen = ((blurredPixel >> 8) & 0xff);
				blurredBlue = (blurredPixel & 0xff);

				//If the absolute val. of difference between original and blurred
				//values are greater than the given threshold add weighed difference
				//back to the original pixel. If the result is outside (0-255),
				//change it back to the corresponding margin 0 or 255
				if (Math.abs(orgRed - blurredRed) >= threshold) {
					orgRed = (int) (amount * (orgRed - blurredRed) + orgRed);
					orgRed = orgRed > 255 ? 255 : orgRed < 0 ? 0 : orgRed;
				}

				if (Math.abs(orgGreen - blurredGreen) >= threshold) {
					orgGreen = (int) (amount * (orgGreen - blurredGreen) + orgGreen);
					orgGreen = orgGreen > 255 ? 255 : orgGreen < 0 ? 0 : orgGreen;
				}

				if (Math.abs(orgBlue - blurredBlue) >= threshold) {
					orgBlue = (int) (amount * (orgBlue - blurredBlue) + orgBlue);
					orgBlue = orgBlue > 255 ? 255 : orgBlue < 0 ? 0 : orgBlue;
				}

				usmPixel = (alpha | (orgRed << 16) | (orgGreen << 8) | orgBlue);
				mOutIntValues[y * w + x] = usmPixel;
			}
		}
		output.setPixels(mOutIntValues, 0, w, 0, 0, w, h);
		if (!blur.isRecycled()) {
			blur.recycle();
		}

		if (!input.isRecycled()) {
			input.recycle();
		}
		return output;
	}

	public static Mat rotateMat(Mat src, double angle)
	{
		Mat dst = new Mat();
		if(angle == 180 || angle == -180) {
			Core.flip(src, dst, -1);
		} else if(angle == 90 || angle == -270) {
			Core.flip(src.t(), dst, 1);
		} else if(angle == 270 || angle == -90) {
			Core.flip(src.t(), dst, 0);
		} else {
			return src;
		}

		return dst;
	}

	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
		if ((degrees != 0 || mirror) && b != null) {
			Matrix m = new Matrix();
			// Mirror first.
			// horizontal flip + rotation = -rotation + horizontal flip
			if (mirror) {
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180) {
					m.postTranslate(b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270) {
					m.postTranslate(b.getHeight(), 0);
				} else {
					throw new IllegalArgumentException("Invalid degrees=" + degrees);
				}
			}
			if (degrees != 0) {
				// clockwise
				m.postRotate(degrees,
						(float) b.getWidth() / 2, (float) b.getHeight() / 2);
			}

			try {
				Bitmap b2 = Bitmap.createBitmap(
						b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2) {
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex) {
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	public static Matrix getTransformationMatrix(
			final int srcWidth,
			final int srcHeight,
			final int dstWidth,
			final int dstHeight,
			final int applyRotation,
			final boolean maintainAspectRatio) {
		final Matrix matrix = new Matrix();

		if (applyRotation != 0) {
			if (applyRotation % 90 != 0) {
			}

			// Translate so center of image is at origin.
			matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

			// Rotate around origin.
			matrix.postRotate(applyRotation);
		}

		// Account for the already applied rotation, if any, and then determine how
		// much scaling is needed for each axis.
		final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

		final int inWidth = transpose ? srcHeight : srcWidth;
		final int inHeight = transpose ? srcWidth : srcHeight;

		// Apply scaling if necessary.
		if (inWidth != dstWidth || inHeight != dstHeight) {
			final float scaleFactorX = dstWidth / (float) inWidth;
			final float scaleFactorY = dstHeight / (float) inHeight;

			if (maintainAspectRatio) {
				// Scale by minimum factor so that dst is filled completely while
				// maintaining the aspect ratio. Some image may fall off the edge.
				final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
				matrix.postScale(scaleFactor, scaleFactor);
			} else {
				// Scale exactly to fill dst from src.
				matrix.postScale(scaleFactorX, scaleFactorY);
			}
		}

		if (applyRotation != 0) {
			// Translate back from origin centered reference to destination frame.
			matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
		}

		return matrix;
	}


	public static Bitmap tfResizeBilinear(Bitmap bitmap, int w, int h) {
		if (bitmap == null) {
			return null;
		}

		Bitmap resized = Bitmap.createBitmap(w, h,
				Config.ARGB_8888);

		final Canvas canvas = new Canvas(resized);
		canvas.drawBitmap(bitmap,
				new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
				new Rect(0, 0, w, h),
				null);

		return resized;
	}

	public static int estimateSampleSize(String filePath,
                                         int destWidth,
                                         int destHeight,
                                         int orientation) {
		if (filePath == null) {
			return 0;
		}

		if (destWidth <= 0 || destHeight <= 0) {
			return 0;
		}

		int sw = 0;
		int sh = 0;

		final Options opts = new Options();
		opts.inJustDecodeBounds = true;

		try {
			BitmapFactory.decodeFile(filePath, opts);
		} catch (OutOfMemoryError e) {
			sw = 0;
			sh = 0;
		}
		
/*		Logger.debug("bitmap = [%-3d x %-3d], thumb = [%-3d x %-3d]", 
				opts.outWidth,
				opts.outHeight,
				tw, th);
*/
		sw = opts.outWidth;
		sh = opts.outHeight;

		if (orientation == 90 || orientation == 270) {
			sw = opts.outHeight;
			sh = opts.outWidth;
		}

		return Math.min(sw / destWidth, sh / destHeight);
	}

	public static Bitmap rotateBitmap(Bitmap source, int degrees) {
		if (degrees != 0 && source != null) {
			Matrix m = new Matrix();

			m.setRotate(degrees, (float) source.getWidth() / 2,
					(float) source.getHeight() / 2);

			try {
				Bitmap rbitmap = Bitmap.createBitmap(
						source,
						0, 0,
						source.getWidth(), source.getHeight(),
						m, true);
				if (source != rbitmap) {
					source.recycle();
					source = rbitmap;
				}
			} catch (OutOfMemoryError ex) {
			}
		}

		return source;
	}

	public static Bitmap scaleBitmapRatioLocked(Bitmap bitmap,
                                                int destWidth,
                                                int destHeight) {
		if (bitmap == null) {
			return null;
		}

		final int destMin = Math.min(destWidth, destHeight);
		if (destMin <= 0) {


			return bitmap;
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int tw;
		int th;
		if (w > h) {
			tw = destMin;
			th = (tw * h / w);
		} else if (w < h) {
			th = destMin;
			tw = (th * w / h);
		} else {
			tw = th = destMin;
		}

		return scaleBitmap(bitmap, tw, th);
	}

	public static Bitmap scaleBitmap(Bitmap bitmap,
                                     int destWidth, int destHeight) {
		if (bitmap == null) {
			return null;
		}
		
		if (destWidth <= 0 || destHeight <= 0) {
			return bitmap;
		}
		
		Bitmap newBitmap = bitmap;

		final int owidth = bitmap.getWidth();
		final int oheight = bitmap.getHeight();

		if (owidth > destWidth) {
			if (oheight > destHeight) {
				float scaleWidth = ((float) destWidth / owidth);
				float scaleHeight = ((float) destHeight / oheight);
				if (scaleWidth > scaleHeight) {
					Bitmap tempBitmap = createScaledBitmap(bitmap, scaleWidth,
							owidth, oheight);
					if (tempBitmap != null) {
						newBitmap = createClippedBitmap(tempBitmap, 0,
								(tempBitmap.getHeight() - destHeight) / 2, destWidth,
								destHeight);
					}
				} else {
					Bitmap tempBitmap = createScaledBitmap(bitmap, scaleHeight,
							owidth, oheight);
					if (tempBitmap != null) {
						newBitmap = createClippedBitmap(tempBitmap,
								(tempBitmap.getWidth() - destWidth) / 2, 0,
								destWidth, destHeight);
					}
				}

			} else {
				newBitmap = createClippedBitmap(bitmap,
						(bitmap.getWidth() - destWidth) / 2, 0, destWidth, oheight);
			}

		} else if (owidth <= destWidth) {
			if (oheight > destHeight) {
				newBitmap = createClippedBitmap(bitmap, 0,
						(bitmap.getHeight() - destHeight) / 2, owidth, destHeight);
			} else {
				newBitmap = Bitmap.createBitmap(destWidth, destHeight, bitmap.getConfig());

				Canvas c = new Canvas(newBitmap);
				Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
				c.drawBitmap(bitmap,
						new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
						new Rect(0, 0, destWidth, destHeight), p);
			}
		}

		return newBitmap;
	}
	
	private static Bitmap createScaledBitmap(Bitmap bitmap, float scale,
                                             int width, int height) {
		if (bitmap == null) {
			return null;
		}
		
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);

		return Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
	}

	public static Bitmap createClippedBitmap(Bitmap bitmap, int x, int y,
                                             int width, int height) {
		if (bitmap == null) {
			return null;
		}

		return Bitmap.createBitmap(bitmap, x, y, width, height);
	}
	
	public static void saveBitmap(Bitmap bitmap, String filename) {
		saveBitmap(bitmap, filename, 85);
	}
	
	public static boolean saveBitmap(Bitmap bitmap, String filename, int quailty) {
		if (filename == null) {
			return false;
		}
		
		File file = new File(filename);
		
		return saveBitmap(bitmap, file, quailty);
	}
	
	public static boolean saveBitmap(Bitmap bitmap, File file) {
		return saveBitmap(bitmap, file, 100);
	}
	
	public static boolean saveBitmap(Bitmap bitmap, File file, int quailty) {
		if (bitmap == null || file == null) {
			return false;
		}
		
		boolean success = false;
		try {
			FileOutputStream out = new FileOutputStream(file);
			CompressFormat format =
					(quailty >= 100 ? CompressFormat.PNG
							: CompressFormat.JPEG);


			final boolean ret = bitmap.compress(format, quailty, out);

			out.flush();
			out.close();

			success = ret;
		} catch (IOException e) {

			success = false;
		}

		return success;
	}

	public static Bitmap createColorFilteredBitmap(Bitmap origBitmap,
                                                   ColorMatrix cm) {
		if (origBitmap == null || cm == null) {
			return origBitmap;
		}

		final int width = origBitmap.getWidth();
		final int height = origBitmap.getHeight();
		if (width <= 0 || height <= 0) {
			return origBitmap;
		}

		Bitmap filteredBitmap = Bitmap.createBitmap(width,
				height, Config.ARGB_8888);

		Canvas c = new Canvas(filteredBitmap);

		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);

	    Paint paint = new Paint();
		paint.setColorFilter(f);

	    c.drawBitmap(origBitmap, 0, 0, paint);

	    return filteredBitmap;
	}

	public static Bitmap createGrayScaledBitmap(Bitmap origBitmap) {
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);

		return createColorFilteredBitmap(origBitmap, cm);
	}

	public static Bitmap createViewSnapshot(Context context,
                                            View view,
                                            int desireWidth, int desireHeight) {
		if (view == null) {
			return null;
		}

		int widthMeasureSpec;
		int heightMeasureSpec;

		if (desireWidth <= 0) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(
					desireWidth, MeasureSpec.UNSPECIFIED);
		} else {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(
					desireWidth, MeasureSpec.EXACTLY);
		}

		if (desireHeight <= 0) {
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(
					desireHeight, MeasureSpec.UNSPECIFIED);
		} else {
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(
					desireHeight, MeasureSpec.EXACTLY);
		}

		view.measure(widthMeasureSpec, heightMeasureSpec);

		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

		Config config = Config.ARGB_8888;

		Bitmap bitmap = null;

		try {
			bitmap = Bitmap.createBitmap(
					desireWidth, desireHeight, config);
		} catch (OutOfMemoryError e) {


			bitmap = null;
		}

		if (bitmap == null) {
			return null;
		}

		Canvas canvas = new Canvas(bitmap);

		view.draw(canvas);

		return bitmap;
	}

	public static String bitmapToBase64String(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = null;
		String base64str = null;
		try {
			bitmap.compress(CompressFormat.PNG, 100, baos);

			bytes = baos.toByteArray();

			base64str = Base64.encodeToString(bytes, Base64.DEFAULT);
		} catch (OutOfMemoryError e) {


			base64str = null;
		}

		return base64str;
	}

    public static Bitmap bitmapFromBase64String(String base64String) {
        if (TextUtils.isEmpty(base64String)) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
            if (bytes != null && bytes.length > 0) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }  catch (OutOfMemoryError e) {


            bitmap = null;
        }

        return bitmap;
    }

    public static Bitmap compositeDrawableWithMask(
            Bitmap rgbBitmap, Bitmap alphaBitmap) {
		if (rgbBitmap == null) {
			return null;
		}

		if (alphaBitmap == null) {
			return rgbBitmap;
		}

		final int rgbw = rgbBitmap.getWidth();
		final int rgbh = rgbBitmap.getHeight();
		final int alphaw = alphaBitmap.getWidth();
		final int alphah = alphaBitmap.getHeight();
		if (rgbw != alphaw
				|| rgbh != alphah) {


			return rgbBitmap;
		}

		Bitmap destBitmap = Bitmap.createBitmap(rgbw, rgbh,
				Config.ARGB_8888);

		int[] pixels = new int[rgbw];
		int[] alpha = new int[rgbw];
		for (int y = 0; y < rgbh; y++) {
			rgbBitmap.getPixels(pixels, 0, rgbw, 0, y, rgbw, 1);
			alphaBitmap.getPixels(alpha, 0, rgbw, 0, y, rgbw, 1);

			for (int x = 0; x < rgbw; x++) {
				// Replace the alpha channel with the r value from the bitmap.
				pixels[x] = (pixels[x] & 0x00FFFFFF)
						| ((alpha[x] << 8) & 0xFF000000);
			}

			destBitmap.setPixels(pixels, 0, rgbw, 0, y, rgbw, 1);
		}

		return destBitmap;
	}

	public static Bitmap compositeBitmaps(Bitmap bitmap1, Bitmap bitmap2) {
		return compositeBitmaps(false, bitmap1, bitmap2);
	}

	public static Bitmap compositeBitmaps(boolean scale, Bitmap bitmap1, Bitmap bitmap2) {
		return compositeBitmaps(scale, new Bitmap[] { bitmap1, bitmap2 });
	}

	public static Bitmap compositeBitmaps(Bitmap... bitmaps) {
		return compositeBitmaps(false, bitmaps);
	}

	public static Bitmap compositeBitmaps(boolean scale, Bitmap... bitmaps) {
		if (bitmaps == null) {
			return null;
		}

		final int N = bitmaps.length;
		if (N == 1) {
			return bitmaps[0];
		}

		if (bitmaps[0] == null) {
			return bitmaps[0];
		}

		int bw = bitmaps[0].getWidth();
		int bh = bitmaps[0].getHeight();
		final Config config = bitmaps[0].getConfig();

		if (!scale) {
			int[] dimension = findMaxDimension(bitmaps);
			if (dimension != null) {
				bw = dimension[0];
				bh = dimension[1];
			}
		}

/*		Logger.debug("target composite dimen: %d x %d",
				bw, bh);
*/
		Bitmap finalBitmap = null;
		try {
			finalBitmap = Bitmap.createBitmap(bw, bh, config);
		} catch (OutOfMemoryError e) {


			finalBitmap = null;
		}

		if (finalBitmap == null) {
			return bitmaps[0];
		}

		Canvas canvas = new Canvas(finalBitmap);

		Bitmap currbmp = null;
		Rect src = new Rect();
		Rect dst = new Rect();
		int xoff = 0;
		int yoff = 0;
		for (int i = 0; i < N; i++) {
			currbmp = bitmaps[i];
			if (currbmp == null) {
				continue;
			}

			xoff = 0;
			yoff = 0;

			if (currbmp.getWidth() != bw
						|| currbmp.getHeight() != bh) {
				if (scale) {
					currbmp = BitmapUtils.scaleBitmap(currbmp, bw, bh);
				} else {
					xoff = (bw - currbmp.getWidth()) / 2;
					yoff = (bh - currbmp.getHeight()) / 2;
				}
			}

			src.set(0, 0, currbmp.getWidth(), currbmp.getHeight());
			dst.set(xoff, yoff,
					xoff + currbmp.getWidth(),
					yoff + currbmp.getHeight());

			canvas.drawBitmap(currbmp, src, dst, null);
		}

		return finalBitmap;
	}

	public static Bitmap loadAssetBitmap(Context context, String assetFile) {
		AssetManager assetManager = context.getAssets();
		if (assetManager == null) {
			return null;
		}

		if (TextUtils.isEmpty(assetFile)) {
			return null;
		}

	    InputStream istream = null;
	    Bitmap bitmap = null;
	    try {
	        istream = assetManager.open(assetFile);

	        if (istream != null) {
	        	bitmap = BitmapFactory.decodeStream(istream);
	        }
	    } catch (OutOfMemoryError e) {


	        bitmap = null;
		} catch (IOException e) {

	        bitmap = null;
	    } finally {
	    	try {
		    	if (istream != null) {
		    		istream.close();
		    	}
	    	} catch (IOException e) {
				e.printStackTrace();
			}
	    }

	    return bitmap;
	}

	public static int[] findMaxDimension(Bitmap... bitmaps) {
		if (bitmaps == null) {
			return null;
		}

		int[] dimension = new int[] {0, 0};

		final int N = bitmaps.length;
		if (N == 1) {
			if (bitmaps[0] != null) {
				dimension[0] = bitmaps[0].getWidth();
				dimension[1] = bitmaps[0].getHeight();

			}
			return dimension;
		}

		Bitmap bitmap = null;
		for (int i = 0; i < N; i++) {
			bitmap = bitmaps[i];
			if (bitmap == null) {
				continue;
			}

			if (bitmap.getWidth() > dimension[0]) {
				dimension[0] = bitmap.getWidth();
			}

			if (bitmap.getHeight() > dimension[1]) {
				dimension[1] = bitmap.getHeight();
			}
		}

		return dimension;
	}

    public static Bitmap getRoundBitmap(Bitmap source, int radius) {
        Bitmap scaledBitmap;
        if(source.getWidth() != radius || source.getHeight() != radius) {
            scaledBitmap = scaleBitmap(source, radius * 2, radius * 2);
        } else {
            scaledBitmap = source;
        }

        Bitmap output = Bitmap.createBitmap(scaledBitmap.getWidth(),
                scaledBitmap.getHeight(), Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Rect rect = new Rect(0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawARGB(0, 0, 0, 0);

        canvas.drawCircle(scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2,
                scaledBitmap.getWidth() / 2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, rect, rect, paint);

        return output;
    }

	public static int calculateBrightnessEstimate(Bitmap bitmap, int pixelSpacing) {
		if (bitmap == null) {
			return 0;
		}

		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if (width <= 0 || height <= 0) {
			return 0;
		}

		int r = 0, g = 0, b = 0, n = 0;

		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		int i, color;
		for (i = 0; i < pixels.length; i += pixelSpacing) {
			color = pixels[i];

			r += Color.red(color);
			g += Color.green(color);
			b += Color.blue(color);

			n++;
		}

		return (r + b + g) / (n * 3);
	}

	public static int calculateBrightness(Bitmap bitmap) {
		return calculateBrightnessEstimate(bitmap, 1);
	}

	public static Bitmap paddingBitmap(Bitmap origin,
                                       int padding,
                                       int paddingBackground,
                                       boolean expand) {
		if (origin == null
				|| padding <= 0) {
			return origin;
		}

		final int w = origin.getWidth();
		final int h = origin.getHeight();



		int destW = w;
		int destH = h;

		if (expand) {
			destW += padding * 2;
			destH += padding * 2;
		}

		Bitmap newOne = Bitmap.createBitmap(destW, destH, Config.ARGB_8888);
		Canvas canvas = new Canvas(newOne);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		canvas.drawColor(paddingBackground);

		canvas.drawBitmap(origin,
				new Rect(0, 0, w, h),
				new Rect(padding, padding, destW - padding, destH - padding),
				paint);

		return newOne;
	}

	public static Bitmap extendBitmap(Bitmap origin, int destW, int destH, int backgroundColor) {
		if (origin == null
				|| destW <= 0
				|| destH <= 0) {
			return origin;
		}

		final int w = origin.getWidth();
		final int h = origin.getHeight();
		if (destW < w || destH < h) {
			return origin;
		}


		Bitmap newOne = Bitmap.createBitmap(destW, destH, Config.ARGB_8888);
		Canvas canvas = new Canvas(newOne);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		canvas.drawColor(backgroundColor);

		final int xOffset = (int) Math.round((destW - w) / 2.0);
		final int yOffset = (int) Math.round((destH - h) / 2.0);
		canvas.drawBitmap(origin, xOffset, yOffset, paint);

		return newOne;
	}

	private final static int DEFAULT_RADIUS = 15;
	private final static int DEFAULT_INTENSITY = 10;

	private static final int[] sIntensityCount = new int[256];
	private static final int[] sSumR = new int[256];
	private static final int[] sSumG = new int[256];
	private static final int[] sSumB = new int[256];

	public static Bitmap oilPaintBitmap(Bitmap bitmap) {
		return oilPaintBitmap(bitmap, DEFAULT_RADIUS, DEFAULT_INTENSITY);
	}

	public static Bitmap oilPaintBitmap(Bitmap bitmap, int radius, int intensity) {
		if (bitmap == null) {
			return null;
		}

		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();

		if (width <= 0 || height <= 0 || radius <= 0 || intensity <= 0) {

			return bitmap;
		}

        long start, end;

		start = System.currentTimeMillis();

		int[] pixels = new int[width * height];

		bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		int[] output = Arrays.copyOf(pixels, pixels.length);

		int cX, cY;
		int mX, mY;
		int color, r, g, b, i;
		int max, maxIndex, mI;
		for (cY = radius; cY < height - radius; cY++) {
			for (cX = radius; cX < width - radius; cX++) {
                Arrays.fill(sIntensityCount, 0);
                Arrays.fill(sSumR, 0);
                Arrays.fill(sSumG, 0);
                Arrays.fill(sSumB, 0);

                for (mY = -radius; mY <= radius; mY++) {
					for (mX = -radius; mX <= radius; mX++) {
						color = pixels[(cX + mX) + (cY + mY) * width];
						r = ((color >> 16) & 0xFF);
						g = ((color >> 8) & 0xFF);
						b = (color & 0xFF);

						i = (int)((((r + g + b) / 3.0) * intensity) / 255);
						if (i > 255) {
							i = 255;
						}

						sIntensityCount[i]++;
						sSumR[i] = sSumR[i] + r;
						sSumG[i] = sSumG[i] + g;
						sSumB[i] = sSumB[i] + b;
					}
				}

				max = 0;
				maxIndex = 0;

				for (mI = 0; mI < 256; mI++) {
					if (sIntensityCount[mI] > max) {
						max = sIntensityCount[mI];
						maxIndex = mI;
					}
				}

				r = sSumR[maxIndex] / max;
				g = sSumG[maxIndex] / max;
				b = sSumB[maxIndex] / max;

				output[(cX) + (cY) * width] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}

		Bitmap dest = Bitmap.createBitmap(width, height, Config.ARGB_8888);

		dest.setPixels(output, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		end = System.currentTimeMillis();


        return dest;
	}

    public static Bitmap concatBitmap(Bitmap bitmap1, Bitmap bitmap2) {
	    if (bitmap1 == null || bitmap2 == null) {
	        return null;
        }

        final int w1 = bitmap1.getWidth();
        final int h1 = bitmap1.getHeight();
        final int w2 = bitmap2.getWidth();
        final int h2 = bitmap2.getHeight();


        final boolean landscape1 = (w1 > h1);
        final boolean landscape2 = (w2 > h2);

        if (landscape1 != landscape2) {


            return null;
        }

		int w, h;
        if (landscape1) {
            w = Math.min(w1, w2);

            if (w1 != w) {
                bitmap1 = scaleBitmapRatioLocked(bitmap1, w, w);
            }

            if (w2 != w) {
                bitmap2 = scaleBitmapRatioLocked(bitmap2, w, w);
            }

            h = bitmap1.getHeight() + bitmap2.getHeight();
        } else {
            h = Math.min(h1, h2);

            if (h1 != h) {
                bitmap1 = scaleBitmapRatioLocked(bitmap1, h, h);
            }

            if (h2 != h) {
                bitmap2 = scaleBitmapRatioLocked(bitmap2, h, h);
            }

            w = bitmap1.getWidth() + bitmap2.getWidth();
        }


        if (bitmap1 == null
                || bitmap2 == null) {
            return null;
        }

        Bitmap newBitmap = null;
        try {
            newBitmap = Bitmap.createBitmap(w, h, bitmap1.getConfig());

            Canvas c = new Canvas(newBitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            if (landscape1) {
                c.drawBitmap(bitmap1,
                        new Rect(0, 0, w, bitmap1.getHeight()),
                        new Rect(0, 0, w, bitmap1.getHeight()), p);
                c.drawBitmap(bitmap2,
                        new Rect(0, 0, w, bitmap2.getHeight()),
                        new Rect(0, bitmap1.getHeight(), w, h), p);
            } else {
                c.drawBitmap(bitmap1,
                        new Rect(0, 0, bitmap1.getWidth(), h),
                        new Rect(0, 0, bitmap1.getWidth(), h), p);
                c.drawBitmap(bitmap2,
                        new Rect(0, 0, bitmap2.getWidth(), h),
                        new Rect(bitmap1.getWidth(), 0, w, h), p);
            }
        } catch (OutOfMemoryError e) {

            newBitmap = null;
        }

        return newBitmap;
    }

	/**
	 *
	 * @param bmp input bitmap
	 * @param contrast 0..10 1 is default
	 * @param brightness -255..255 0 is default
	 * @return new bitmap
	 */
	public static Bitmap changeBitmapContrastBrightness(Context context, Bitmap bmp, float contrast, float brightness)
	{
		Log.d(TAG, "changeBitmapContrastBrightness: start");
		ColorMatrix cm = new ColorMatrix(new float[]
				{
						contrast, 0, 0, 0, brightness,
						0, contrast, 0, 0, brightness,
						0, 0, contrast, 0, brightness,
						0, 0, 0, 1, 0
				});

		Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

		Canvas canvas = new Canvas(ret);

		Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(cm));
		canvas.drawBitmap(bmp, 0, 0, paint);

		Log.d(TAG, "changeBitmapContrastBrightness: end");
		return USM(context, bmp, 3.45f, 0.4f);
	}

	public static byte[] getByteArrayFromBitmap(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 96, stream);
		return stream.toByteArray();

	}

	public static byte[] getByteArrayFromBitmapQuality(Bitmap bitmap, int quality) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, quality, stream);
		return stream.toByteArray();

	}

	public static byte[] getByteArrayFromBitmapWithPNG(Bitmap bitmap,int quality) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, quality, stream);
		return stream.toByteArray();

	}

	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	public static final float[] LOW_SHARP = { -0.06f, -0.06f, -0.06f, -0.06f, 1.48f, -0.06f, -0.06f, -0.06f, -0.06f};
	public static final float[] MEDIUM_SHARP = { -0.22f, -0.22f, -0.22f, -0.22f, 2.76f, -0.22f, -0.22f, -0.22f, -0.22f};

	public static Bitmap doSharpen(Context context, Bitmap original, float[] radius) {
		Log.d(TAG, "doSharpen: start");
		Bitmap bitmap = Bitmap.createBitmap(
				original.getWidth(), original.getHeight(),
				Config.ARGB_8888);

		RenderScript rs = RenderScript.create(context);

		Allocation allocIn = Allocation.createFromBitmap(rs, original);
		Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

		ScriptIntrinsicConvolve3x3 convolution
				= ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
		convolution.setInput(allocIn);
		convolution.setCoefficients(radius);
		convolution.forEach(allocOut);

		allocOut.copyTo(bitmap);
		rs.destroy();
		Log.d(TAG, "doSharpen: end");
		return bitmap;

	}

}
