package com.company;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.cedd.*;
import net.semanticmetadata.lire.utils.ImageUtils;
import net.semanticmetadata.lire.utils.SerializationUtils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * The CEDD feature was created, implemented and provided by Savvas A.
 * Chatzichristofis<br/>
 * More information can be found in: Savvas A. Chatzichristofis and Yiannis S.
 * Boutalis, <i>CEDD: Color and Edge Directivity Descriptor. A Compact
 * Descriptor for Image Indexing and Retrieval</i>, A. Gasteratos, M. Vincze,
 * and J.K. Tsotsos (Eds.): ICVS 2008, LNCS 5008, pp. 312-322, 2008.
 * 
 * @author: Savvas A. Chatzichristofis, savvash@gmail.com
 */
public class Cedd implements LireFeature {
	private double T0;
	private double T1;
	private double T2;
	private double T3;
	private boolean Compact = false;
	// protected double[] data = new double[144];
	protected byte[] histogram = new byte[144];

	int tmp;
	// for tanimoto:
	private double Result, Temp1, Temp2, TempCount1, TempCount2, TempCount3;
	private Cedd tmpFeature;
	private double iTmp1, iTmp2;

	public Cedd(double Th0, double Th1, double Th2, double Th3,
			boolean CompactDescriptor) {
		this.T0 = Th0;
		this.T1 = Th1;
		this.T2 = Th2;
		this.T3 = Th3;
		this.Compact = CompactDescriptor;
	}

	public Cedd() {
		this.T0 = 14d;
		this.T1 = 0.68d;
		this.T2 = 0.98d;
		this.T3 = 0.98d;
	}

	// Apply filter
	// signature changed by mlux
	public void extract(BufferedImage image) {
		image = ImageUtils.get8BitRGBImage(image);
		Fuzzy10Bin Fuzzy10 = new Fuzzy10Bin(false);
		Fuzzy24Bin Fuzzy24 = new Fuzzy24Bin(false);
		RGB2HSV HSVConverter = new RGB2HSV();
		int[] HSV = new int[3];

		double[] Fuzzy10BinResultTable = new double[10];
		double[] Fuzzy24BinResultTable = new double[24];
		double[] CEDD = new double[144];

		int width = image.getWidth();
		int height = image.getHeight();

		double[][] ImageGrid = new double[width][height];
		double[][] PixelCount = new double[2][2];
		int[][] ImageGridRed = new int[width][height];
		int[][] ImageGridGreen = new int[width][height];
		int[][] ImageGridBlue = new int[width][height];

		// please double check from here
		int NumberOfBlocks = -1;

		if (Math.min(width, height) >= 80)
			NumberOfBlocks = 1600;
		if (Math.min(width, height) < 80 && Math.min(width, height) >= 40)
			NumberOfBlocks = 400;
		if (Math.min(width, height) < 40)
			NumberOfBlocks = -1;

		int Step_X = 2;
		int Step_Y = 2;

		if (NumberOfBlocks > 0) {
			Step_X = (int) Math.floor(width / Math.sqrt(NumberOfBlocks));
			Step_Y = (int) Math.floor(height / Math.sqrt(NumberOfBlocks));

			if ((Step_X % 2) != 0) {
				Step_X = Step_X - 1;
			}
			if ((Step_Y % 2) != 0) {
				Step_Y = Step_Y - 1;
			}

		}

		// to here

		int[] Edges = new int[6];

		MaskResults MaskValues = new MaskResults();
		Neighborhood PixelsNeighborhood = new Neighborhood();

		for (int i = 0; i < 144; i++) {
			CEDD[i] = 0;
		}
		int pixel;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixel = image.getRGB(x, y);
				ImageGridRed[x][y] = (pixel >> 16) & 0xff;
				ImageGridGreen[x][y] = (pixel >> 8) & 0xff;
				ImageGridBlue[x][y] = (pixel) & 0xff;
				// int mean = (int) (0.114 * ImageGridBlue[x][y] + 0.587 *
				// ImageGridGreen[x][y] + 0.299 * ImageGridRed[x][y]);
				// ImageGrid[x][y] = (0.114f * ImageGridBlue[x][y] + 0.587f *
				// ImageGridGreen[x][y] + 0.299f * ImageGridRed[x][y]);
				ImageGrid[x][y] = (0.299f * ((pixel >> 16) & 0xff) + 0.587f
						* ((pixel >> 8) & 0xff) + 0.114f * ((pixel) & 0xff));

			}
		}

		int[] CororRed = new int[Step_Y * Step_X];
		int[] CororGreen = new int[Step_Y * Step_X];
		int[] CororBlue = new int[Step_Y * Step_X];

		int[] CororRedTemp = new int[Step_Y * Step_X];
		int[] CororGreenTemp = new int[Step_Y * Step_X];
		int[] CororBlueTemp = new int[Step_Y * Step_X];

		int MeanRed, MeanGreen, MeanBlue;

		// plase double check from here

		int TempSum = 0;
		double Max = 0;

		int TemoMAX_X = Step_X * (int) Math.floor(image.getWidth() >> 1);
		int TemoMAX_Y = Step_Y * (int) Math.floor(image.getHeight() >> 1);

		if (NumberOfBlocks > 0) {
			TemoMAX_X = Step_X * (int) Math.sqrt(NumberOfBlocks);
			TemoMAX_Y = Step_Y * (int) Math.sqrt(NumberOfBlocks);
		}

		// to here

		for (int y = 0; y < TemoMAX_Y; y += Step_Y) {
			for (int x = 0; x < TemoMAX_X; x += Step_X) {

				MeanRed = 0;
				MeanGreen = 0;
				MeanBlue = 0;
				PixelsNeighborhood.Area1 = 0;
				PixelsNeighborhood.Area2 = 0;
				PixelsNeighborhood.Area3 = 0;
				PixelsNeighborhood.Area4 = 0;
				Edges[0] = -1;
				Edges[1] = -1;
				Edges[2] = -1;
				Edges[3] = -1;
				Edges[4] = -1;
				Edges[5] = -1;

				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 2; j++) {
						PixelCount[i][j] = 0;
					}
				}

				TempSum = 0;

				for (int i = y; i < y + Step_Y; i++) {
					for (int j = x; j < x + Step_X; j++) {

						CororRed[TempSum] = ImageGridRed[j][i];
						CororGreen[TempSum] = ImageGridGreen[j][i];
						CororBlue[TempSum] = ImageGridBlue[j][i];

						CororRedTemp[TempSum] = ImageGridRed[j][i];
						CororGreenTemp[TempSum] = ImageGridGreen[j][i];
						CororBlueTemp[TempSum] = ImageGridBlue[j][i];

						TempSum++;

						if (j < (x + Step_X / 2) && i < (y + Step_Y / 2))
							PixelsNeighborhood.Area1 += (ImageGrid[j][i]);
						if (j >= (x + Step_X / 2) && i < (y + Step_Y / 2))
							PixelsNeighborhood.Area2 += (ImageGrid[j][i]);
						if (j < (x + Step_X / 2) && i >= (y + Step_Y / 2))
							PixelsNeighborhood.Area3 += (ImageGrid[j][i]);
						if (j >= (x + Step_X / 2) && i >= (y + Step_Y / 2))
							PixelsNeighborhood.Area4 += (ImageGrid[j][i]);

					}
				}

				PixelsNeighborhood.Area1 = (int) (PixelsNeighborhood.Area1 * (4.0 / (Step_X * Step_Y)));

				PixelsNeighborhood.Area2 = (int) (PixelsNeighborhood.Area2 * (4.0 / (Step_X * Step_Y)));

				PixelsNeighborhood.Area3 = (int) (PixelsNeighborhood.Area3 * (4.0 / (Step_X * Step_Y)));

				PixelsNeighborhood.Area4 = (int) (PixelsNeighborhood.Area4 * (4.0 / (Step_X * Step_Y)));

				MaskValues.Mask1 = Math.abs(PixelsNeighborhood.Area1 * 2
						+ PixelsNeighborhood.Area2 * -2
						+ PixelsNeighborhood.Area3 * -2
						+ PixelsNeighborhood.Area4 * 2);
				MaskValues.Mask2 = Math.abs(PixelsNeighborhood.Area1 * 1
						+ PixelsNeighborhood.Area2 * 1
						+ PixelsNeighborhood.Area3 * -1
						+ PixelsNeighborhood.Area4 * -1);
				MaskValues.Mask3 = Math.abs(PixelsNeighborhood.Area1 * 1
						+ PixelsNeighborhood.Area2 * -1
						+ PixelsNeighborhood.Area3 * 1
						+ PixelsNeighborhood.Area4 * -1);
				MaskValues.Mask4 = Math.abs(PixelsNeighborhood.Area1
						* Math.sqrt(2) + PixelsNeighborhood.Area2 * 0
						+ PixelsNeighborhood.Area3 * 0
						+ PixelsNeighborhood.Area4 * -Math.sqrt(2));
				MaskValues.Mask5 = Math.abs(PixelsNeighborhood.Area1 * 0
						+ PixelsNeighborhood.Area2 * Math.sqrt(2)
						+ PixelsNeighborhood.Area3 * -Math.sqrt(2)
						+ PixelsNeighborhood.Area4 * 0);

				Max = Math.max(MaskValues.Mask1, Math.max(MaskValues.Mask2,
						Math.max(MaskValues.Mask3, Math.max(MaskValues.Mask4,
								MaskValues.Mask5))));

				MaskValues.Mask1 = MaskValues.Mask1 / Max;
				MaskValues.Mask2 = MaskValues.Mask2 / Max;
				MaskValues.Mask3 = MaskValues.Mask3 / Max;
				MaskValues.Mask4 = MaskValues.Mask4 / Max;
				MaskValues.Mask5 = MaskValues.Mask5 / Max;

				int T = -1;

				if (Max < T0) {
					Edges[0] = 0;
					T = 0;
				} else {
					T = -1;

					if (MaskValues.Mask1 > T1) {
						T++;
						Edges[T] = 1;
					}
					if (MaskValues.Mask2 > T2) {
						T++;
						Edges[T] = 2;
					}
					if (MaskValues.Mask3 > T2) {
						T++;
						Edges[T] = 3;
					}
					if (MaskValues.Mask4 > T3) {
						T++;
						Edges[T] = 4;
					}
					if (MaskValues.Mask5 > T3) {
						T++;
						Edges[T] = 5;
					}

				}

				for (int i = 0; i < (Step_Y * Step_X); i++) {
					MeanRed += CororRed[i];
					MeanGreen += CororGreen[i];
					MeanBlue += CororBlue[i];
				}

				MeanRed = (int) (MeanRed / (Step_Y * Step_X));
				MeanGreen = (int) (MeanGreen / (Step_Y * Step_X));
				MeanBlue = (int) (MeanBlue / (Step_Y * Step_X));

				HSV = HSVConverter.ApplyFilter(MeanRed, MeanGreen, MeanBlue);

				if (this.Compact == false) {
					Fuzzy10BinResultTable = Fuzzy10.ApplyFilter(HSV[0], HSV[1],
							HSV[2], 2);
					Fuzzy24BinResultTable = Fuzzy24.ApplyFilter(HSV[0], HSV[1],
							HSV[2], Fuzzy10BinResultTable, 2);

					for (int i = 0; i <= T; i++) {
						for (int j = 0; j < 24; j++) {
							if (Fuzzy24BinResultTable[j] > 0)
								CEDD[24 * Edges[i] + j] += Fuzzy24BinResultTable[j];
						}
					}
				} else {
					Fuzzy10BinResultTable = Fuzzy10.ApplyFilter(HSV[0], HSV[1],
							HSV[2], 2);
					for (int i = 0; i <= T; i++) {
						for (int j = 0; j < 10; j++) {
							if (Fuzzy10BinResultTable[j] > 0)
								CEDD[10 * Edges[i] + j] += Fuzzy10BinResultTable[j];
						}
					}
				}
			}
		}

		double Sum = 0;
		for (int i = 0; i < 144; i++) {
			Sum += CEDD[i];
		}

		for (int i = 0; i < 144; i++) {
			CEDD[i] = CEDD[i] / Sum;
		}

		double qCEDD[];

		if (Compact == false) {
			qCEDD = new double[144];
			CEDDQuant quants = new CEDDQuant();
			qCEDD = quants.Apply(CEDD);
		} else {
			qCEDD = new double[60];
			CompactCEDDQuant quants = new CompactCEDDQuant();
			qCEDD = quants.Apply(CEDD);
		}

		// for (int i = 0; i < qCEDD.length; i++)
		// System.out.println(qCEDD[i]);

		// data = qCEDD; // changed by mlux
		for (int i = 0; i < qCEDD.length; i++) {
			histogram[i] = (byte) qCEDD[i];
		}
	}

	public float getDistance(LireFeature vd) { // added by mlux
		// Check if instance of the right class ...
		if (!(vd instanceof Cedd))
			throw new UnsupportedOperationException("Wrong descriptor.");

		// casting ...
		tmpFeature = (Cedd) vd;

		// check if parameters are fitting ...
		if ((tmpFeature.histogram.length != histogram.length))
			throw new UnsupportedOperationException(
					"Histogram lengths or color spaces do not match");

		// Init Tanimoto coefficient
		Result = 0;
		Temp1 = 0;
		Temp2 = 0;
		TempCount1 = 0;
		TempCount2 = 0;
		TempCount3 = 0;

		for (int i = 0; i < tmpFeature.histogram.length; i++) {
			Temp1 += tmpFeature.histogram[i];
			Temp2 += histogram[i];
		}

		if (Temp1 == 0 && Temp2 == 0)
			return 0f;
		if (Temp1 == 0 || Temp2 == 0)
			return 100f;

		for (int i = 0; i < tmpFeature.histogram.length; i++) {
			iTmp1 = tmpFeature.histogram[i] / Temp1;
			iTmp2 = histogram[i] / Temp2;
			TempCount1 += iTmp1 * iTmp2;
			TempCount2 += iTmp2 * iTmp2;
			TempCount3 += iTmp1 * iTmp1;

		}

		Result = (100 - 100 * (TempCount1 / (TempCount2 + TempCount3 - TempCount1)));
		return (float) Result;

	}

	@SuppressWarnings("unused")
	private double scalarMult(double[] a, double[] b) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}

	public byte[] getByteHistogram() {
		return histogram;
	}

	public String getStringRepresentation() { // added by mlux
		StringBuilder sb = new StringBuilder(histogram.length * 2 + 25);
		sb.append("cedd");
		sb.append(' ');
		sb.append(histogram.length);
		sb.append(' ');
		for (byte aData : histogram) {
			sb.append((int) aData);
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	public void setStringRepresentation(String s) { // added by mlux
		StringTokenizer st = new StringTokenizer(s);
		if (!st.nextToken().equals("cedd"))
			throw new UnsupportedOperationException(
					"This is not a CEDD descriptor.");
		for (int i = 0; i < histogram.length; i++) {
			if (!st.hasMoreTokens())
				throw new IndexOutOfBoundsException(
						"Too few numbers in string representation.");
			histogram[i] = (byte) Integer.parseInt(st.nextToken());
		}

	}

	/**
	 * Provides a much faster way of serialization.
	 * 
	 * @return a byte array that can be read with the corresponding method.
	 * @see net.semanticmetadata.lire.imageanalysis.CEDD#setByteArrayRepresentation(byte[])
	 */
	public byte[] getByteArrayRepresentation() {
		// find out the position of the beginning of the trailing zeros.
		int position = -1;
		for (int i = 0; i < histogram.length; i++) {
			if (position == -1) {
				if (histogram[i] == 0)
					position = i;
			} else if (position > -1) {
				if (histogram[i] != 0)
					position = -1;
			}
		}
		if (position < 0)
			position = 143;
		// find out the actual length. two values in one byte, so we have to
		// round up.
		int length = (position + 1) / 2;
		if ((position + 1) % 2 == 1)
			length = position / 2 + 1;
		byte[] result = new byte[length];
		for (int i = 0; i < result.length; i++) {
			tmp = ((int) (histogram[(i << 1)])) << 4;
			tmp = (tmp | ((int) (histogram[(i << 1) + 1])));
			result[i] = (byte) (tmp - 128);
		}
		return result;
	}

	/**
	 * Reads descriptor from a byte array. Much faster than the String based
	 * method.
	 * 
	 * @param in
	 *            byte array from corresponding method
	 * @see net.semanticmetadata.lire.imageanalysis.CEDD#getByteArrayRepresentation
	 */
	public void setByteArrayRepresentation(byte[] in) {
		setByteArrayRepresentation(in, 0, in.length);
	}

	public void setByteArrayRepresentation(byte[] in, int offset, int length) {
		if ((length << 1) < histogram.length)
			Arrays.fill(histogram, length << 1, histogram.length, (byte) 0);
		for (int i = offset; i < offset + length; i++) {
			tmp = in[i] + 128;
			histogram[((i - offset) << 1) + 1] = ((byte) (tmp & 0x000F));
			histogram[(i - offset) << 1] = ((byte) (tmp >> 4));
		}
	}

	public double[] getDoubleHistogram() {
		return SerializationUtils.castToDoubleArray(histogram);
	}

	@Override
	public String getFeatureName() {
		return "Cedd";
	}

	@Override
	public String getFieldName() {
		return DocumentBuilder.FIELD_NAME_CEDD;
	}
}
