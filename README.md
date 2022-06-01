# RadiomicsJ
Java library to compute radiomics features.
The RadiomicsJ computes 170 or more imaging features.
It is using the imagej for the image processing backend.

## Environment

- Maven3
- ImageJ 1.53 or above
- VIB_ libs from Fiji.
- Java AdoptOpenJDK1.8 or 1.11.

## Image-Preprocessing

### Resampling(2D/3D)

RadiomicsJ performs resampling with:
- [2D]  
	Bilinear  
	Bicubic  
	NearestNeighbor2D  
- [3D]  
	Trilinear  
	NearestNeighbor3D  
	NoInterpolation3D (processing NearestNeighbor2D 3 times for x,y,z)

### Range filtering

User can set voxel value range in unit of density [ range min <= v <= range max ].  
For example, -1000 HU to min , 400 HU to max for CT images.  
Range filtering was performed before remove outliers.  

### Remove outliers

If you wants removing outliers, RadiomicsJ remove out of range of [mean - z score × sd] < v < [mean + z score × sd] from mask.  
Mean value is caluculated by voxels in ROI.  
Default value of the z score is 3 (99.73%CI).  

### Image Filtering

User can use any image filter that implemented in ImageJ/Fiji and these plugins.

### Image Standardization

RadiomicsJ has standardization function (v-mean/sd).  
Mean and SD values are caluculated by voxels in ROI.  
User can set standardization on your needs.  

## Feature family
- Morphological features
- Local intensity features
- Intensity-based statistical features
- Intensity histogram features
- Intensity-volume histogram features
- Grey level co-occurrence based features
- Grey level run length based features
- Grey level size zone based features
- Grey level distance zone based features
- Neighbourhood grey tone difference based features
- Neighbouring grey level dependence based features
- Fractal based features (not presented in IBSI)
- Shape2D features
- (developping)Homological features (not presented in IBSI)  

### 3D basis

3D based radiomics features are calculated by using both series of images and series of masks.  
Masks are sharing voxel size and geometry with a paired image.  

#### Aggregation for textures

RadiomicsJ aggregates by a mean value for each angles (3D, averaging) to compute GLCM, GLRLM features on the 3D configration. 

#### (Optional) 2D basis

In almost cases, meanings of 2D features were included in 3D fetures, that is inducing correlation sometimes.  
With this reason, RadiomicsJ does not including Shape2DFeatures as default.  
It is enable by both force2D=true and enableShape2D=true.  

2D based radiomics features are calculated by using a single slice pair both image slice and mask slice.  
A mask is sharing pixel size and geometry with a paired image.  

As more details, see following correspondence tables.  

## IBSI Correspondence table

### 2D

<table border="1"; style="border-collapse: collapse;　margin-left: auto; margin-right: auto; text-align: center;">
  <!-- header -->
  <tr>
  	<th> Feature Family </th>
  	<th> Feature Name </th>
  	<th> IBSI Feature Code </th>
  </tr>
  <!-- contents -->
  <!-- 2D shape features -->
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> Pixel Surface </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> Perimeter </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> Perimeter to Surface ratio </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> Sphericity </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> Circularity </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> SphericalDisproportion </td><td> - </td></tr>
  <tr bgcolor="#ffffff"><td> 2D Shape Features </td><td> FerretAngle </td><td> - </td></tr>
</table>

### 3D

<table border="1"; style="border-collapse: collapse;　margin-left: auto; margin-right: auto; text-align: center;" >
  <!-- header -->
  <tr>
  	<th> Feature Family </th>
  	<th> Feature Name </th>
  	<th> IBSI Feature Code </th>
  </tr>
  <!-- 3D shape features -->
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume (mesh) </td><td> RNU0 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Volume (voxel counting) </td><td> YEKZ </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Surface area (mesh) </td><td> C0JK </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Surface to volume ratio </td><td> 2PR5 </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Compactness 1 </td><td> SKGS </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Compactness 2 </td><td> BQWJ </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Spherical disproportion </td><td> KRCK </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Sphericity </td><td> QCFX </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Asphericity </td><td> 25C7 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Centre of mass shift </td><td> KLMA </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Maximum 3D diameter </td><td> L0JK </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Major axis length </td><td> TDIC </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Minor axis length </td><td> P9VJ </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Least axis length </td><td> 7J51 </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Elongation </td><td> Q3CK </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Flatness </td><td> N17B </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume density (axis-aligned bounding box) </td><td> PBX1 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Area density (axis-aligned bounding box) </td><td> R57B </td></tr>
  <!-- tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume density (oriented minimum bounding box) </td><td> ZH1A </td></tr -->
  <!-- tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Area density (oriented minimum bounding box) </td><td> IQYR </td></tr -->
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume density (approximate enclosing ellipsoid) </td><td> 6BDE </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Area density (approximate enclosing ellipsoid) </td><td> RDD2 </td></tr>
  <!-- tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume density (minimum volume enclosing ellipsoid) </td><td> SWZ1 </td></tr -->
  <!-- tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Area density (minimum volume enclosing ellipsoid) </td><td> BRI8 </td></tr -->
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Volume density (convex hull) </td><td> R3ER </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Area density (convex hull) </td><td> 7T7F </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Integrated intensity </td><td> 99N0 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Morphological features </td><td> Moran’s I index </td><td> N365 </td></tr>
  <tr bgcolor="#ffffff"><td> Morphological features </td><td> Geary’s C measure </td><td> NPT7 </td></tr>
  <!-- Local intensity features -->
  <tr bgcolor="#e6e6e6"><td> Local intensity features </td><td> Local intensity peak </td><td> VJGA </td></tr>
  <tr bgcolor="#ffffff"><td> Local intensity features </td><td> Global intensity peak </td><td> 0F91 </td></tr>
  <!-- Intensity-based statistical features -->
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Mean intensity </td><td> Q4LE </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Intensity variance </td><td> ECT3 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Intensity skewness </td><td> KE2A </td></tr>
  <!-- tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> (Excess) intensity kurtosis </td><td> IPH6 </td></tr -->
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> intensity kurtosis </td><td> - </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Median intensity </td><td> Y12H </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Minimum intensity </td><td> 1GSF </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> 10th intensity percentile </td><td> QG58 </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> 90th intensity percentile </td><td> 8DWT </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Maximum intensity </td><td> 84IY </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Intensity interquartile range </td><td> SALO </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Intensity range </td><td> 2OJQ </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Intensity-based mean absolute deviation </td><td> 4FUA </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Intensity-based robust mean absolute deviation </td><td> 1128 </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Intensity-based median absolute deviation </td><td> N72L </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Intensity-based coefficient of variation </td><td> 7TET </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Intensity-based quartile coefficient of dispersion </td><td> 9S40 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-based statistical features </td><td> Intensity-based energy </td><td> N8CA </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-based statistical features </td><td> Root mean square intensity </td><td> 5ZWQ </td></tr>
  <!-- Intensity histogram features -->
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Mean discretised intensity </td><td> X6K6 </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Discretised intensity variance </td><td> CH89 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Discretised intensity skewness </td><td> 88K1 </td></tr>
  <!-- tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> (Excess) discretised intensity kurtosis </td><td> C3I7 </td></tr -->
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> discretised intensity kurtosis </td><td> - </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Median discretised intensity </td><td> WIFQ </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Minimum discretised intensity </td><td> 1PR8 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> 10th discretised intensity percentile </td><td> GPMT </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> 90th discretised intensity percentile </td><td> OZ0C </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Maximum discretised intensity </td><td> 3NCY </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Intensity histogram mode </td><td> AMMC </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Discretised intensity interquartile range </td><td> WR0O </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Discretised intensity range </td><td> 5Z3W </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Intensity histogram mean absolute deviation </td><td> D2ZX </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Intensity histogram robust mean absolute deviation </td><td> WRZB </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Intensity histogram median absolute deviation </td><td> 4RNL </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Intensity histogram coefficient of variation </td><td> CWYJ </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Intensity histogram quartile coefficient of dispersion </td><td> SLWD </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Discretised intensity entropy </td><td> TLU2 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Discretised intensity uniformity </td><td> BJ5W </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Maximum histogram gradient </td><td> 12CE </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Maximum histogram gradient intensity </td><td> 8E6O </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity histogram features </td><td> Minimum histogram gradient </td><td> VQB3 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity histogram features </td><td> Minimum histogram gradient intensity </td><td> RHQZ </td></tr>
  <!-- Intensity-volume histogram features -->
  <tr bgcolor="#ffffff"><td> Intensity-volume histogram features </td><td> Volume at intensity fraction </td><td> BC2M </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-volume histogram features </td><td> Intensity at volume fraction </td><td> GBPN </td></tr>
  <tr bgcolor="#ffffff"><td> Intensity-volume histogram features </td><td> Volume fraction difference between intensity fractions </td><td> DDTU </td></tr>
  <tr bgcolor="#e6e6e6"><td> Intensity-volume histogram features </td><td> Intensity fraction difference between volume fractions </td><td> CNV2 </td></tr>
  <!-- tr bgcolor="#ffffff"><td> Intensity-volume histogram features </td><td> Area under the IVH curve </td><td> 9CMM </td></tr -->
  <!-- Grey level co-occurrence based features -->
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Joint maximum </td><td> GYBY </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Joint average </td><td> 60VM </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Joint variance </td><td> UR99 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Joint entropy </td><td> TU9B </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Difference average </td><td> TF7R </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Difference variance </td><td> D3YU </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Difference entropy </td><td> NTRS </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Sum average </td><td> ZGXS </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Sum variance </td><td> OEEB </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Sum entropy </td><td> P6QZ </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Angular second moment(also called energy, uniformity) </td><td> 8ZQL </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Contrast </td><td> ACUI </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Dissimilarity </td><td> 8S9J </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Inverse difference </td><td> IB1Z </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Normalised inverse difference </td><td> NDRX </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Inverse difference moment </td><td> WF0Z </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Normalised inverse difference moment </td><td> 1QCO </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Inverse variance </td><td> E8JP </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Correlation </td><td> NI2N </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Autocorrelation </td><td> QWB0 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Cluster tendency </td><td> DG8W </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Cluster shade </td><td> 7NFM </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Cluster prominence </td><td> AE86 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level co-occurrence based features </td><td> Information correlation 1 </td><td> R8DG </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level co-occurrence based features </td><td> Information correlation 2 </td><td> JN9H </td></tr>
  <!-- Grey level run length based features -->
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Short runs emphasis </td><td> 22OV </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Long runs emphasis </td><td> W4KF </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Low grey level run emphasis </td><td> V3SW </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> High grey level run emphasis </td><td> G3QZ </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Short run low grey level emphasis </td><td> HTZT </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Short run high grey level emphasis </td><td> GD3A </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Long run low grey level emphasis </td><td> IVPO </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Long run high grey level emphasis </td><td> 3KUM </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Grey level non-uniformity </td><td> R5YN </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Normalised grey level non-uniformity </td><td> OVBL </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Run length non-uniformity </td><td> W92Y </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Normalised run length non-uniformity </td><td> IC23 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Run percentage </td><td> 9ZK5 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Grey level variance </td><td> 8CE5 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level run length based features </td><td> Run length variance </td><td> SXLW </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level run length based features </td><td> Run entropy </td><td> HJ9O </td></tr>
  <!-- Grey level size zone based features -->
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Small zone emphasis </td><td> 5QRC </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Large zone emphasis </td><td> 48P8 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Low grey level zone emphasis </td><td> XMSY </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> High grey level zone emphasis </td><td> 5GN9 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Small zone low grey level emphasis </td><td> 5RAI </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Small zone high grey level emphasis </td><td> HW1V </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Large zone low grey level emphasis </td><td> YH51 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Large zone high grey level emphasis </td><td> J17V </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Grey level non-uniformity </td><td> JNSA </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Normalised grey level non-uniformity </td><td> Y1RO </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Zone size non-uniformity </td><td> 4JP3 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Normalised zone size non-uniformity </td><td> VB3A </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Zone percentage </td><td> P30P </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Grey level variance </td><td> BYLV </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level size zone based features </td><td> Zone size variance </td><td> 3NSA </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level size zone based features </td><td> Zone size entropy </td><td> GU8N </td></tr>
  <!-- Grey level distance zone based features -->
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Small distance emphasis </td><td> 0GBI </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Large distance emphasis </td><td> MB4I </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Low grey level zone emphasis </td><td> S1RA </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> High grey level zone emphasis </td><td> K26C </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Small distance low grey level emphasis </td><td> RUVG </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Small distance high grey level emphasis </td><td> DKNJ </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Large distance low grey level emphasis </td><td> A7WM </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Large distance high grey level emphasis </td><td> KLTH </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Grey level non-uniformity </td><td> VFT7 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Normalised grey level non-uniformity </td><td> 7HP3 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Zone distance non-uniformity </td><td> V294 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Normalised zone distance non-uniformity </td><td> IATH </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Zone percentage </td><td> VIWW </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Grey level variance </td><td> QK93 </td></tr>
  <tr bgcolor="#ffffff"><td> Grey level distance zone based features </td><td> Zone distance variance </td><td> 7WT1 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Grey level distance zone based features </td><td> Zone distance entropy </td><td> GBDU </td></tr>
  <!-- Neighbourhood grey tone difference based features -->
  <tr bgcolor="#ffffff"><td> Neighbourhood grey tone difference based features </td><td> Coarseness </td><td> QCDE </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbourhood grey tone difference based features </td><td> Contrast </td><td> 65HE </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbourhood grey tone difference based features </td><td> Busyness </td><td> NQ30 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbourhood grey tone difference based features </td><td> Complexity </td><td> HDEZ </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbourhood grey tone difference based features </td><td> Strength </td><td> 1X9X </td></tr>
  <!-- Neighbouring grey level dependence based features -->
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Low dependence emphasis </td><td> SODN </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> High dependence emphasis </td><td> IMOQ </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Low grey level count emphasis </td><td> TL9H </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> High grey level count emphasis </td><td> OAE7 </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Low dependence low grey level emphasis </td><td> EQ3F </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> Low dependence high grey level emphasis </td><td> JA6D </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> High dependence low grey level emphasis </td><td> NBZI </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> High dependence high grey level emphasis </td><td> 9QMG </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Grey level non-uniformity </td><td> FP8K </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> Normalised grey level non-uniformity </td><td> 5SPA </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Dependence count non-uniformity </td><td> Z87G </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> Normalised dependence count non-uniformity </td><td> OKJI </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Dependence count percentage </td><td> 6XV8 </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> Grey level variance </td><td> 1PFV </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Dependence count variance </td><td> DNX2 </td></tr>
  <tr bgcolor="#ffffff"><td> Neighbouring grey level dependence based features </td><td> Dependence count entropy </td><td> FCBV </td></tr>
  <tr bgcolor="#e6e6e6"><td> Neighbouring grey level dependence based features </td><td> Dependence count energy </td><td> CAS9 </td></tr>
  <!-- Fractal features -->
  <tr bgcolor="#ffffff"><td> Fractal features </td><td> Capacity(Fractal dimension) </td><td> - </td></tr>
</table>

# How to use

If you wants quick example with GUI, see [RadiomicsJ IJ-PlugIn](https://sites.google.com/vis-ionary.com/public/ij-plugin_radiomicsj).  
Else,  

```  
int targetLabel = 1;
ImagePlus images = new ImagePlus(path to images);
ImagePlus masks = new ImagePlus(path to masks);// has label as 1.
RadiomicsJ radiomics = new RadiomicsJ();
radiomics.setDebug(true);//to watch progress.
//also you can use settings properties file.
//radiomics.loadSettings(FilePath to settings.properties);
ResultsTable res = radiomics.execute(imgDir, maskDir, targetLabel);
res.show(RadiomicsJ.resultWindowTitle);
```

# Development

You needs following external libs,
- jogamp-fat.jar([here](https://drive.google.com/file/d/1rDWfyYRUOxQoh4RVsjZx12Je965WPi8e/view?usp=sharing))
- nifti_io.jar ([from imagej plugins](https://imagej.nih.gov/ij/plugins/download/jars/))

and add to build path these libs.

# Acknowledgments

RadiomicsJ is referencing some great radiomics libraries, such as listed on IBSI contributors.
RadiomicsJ would like to thank PyRadiomics for great instructions and documentations.  
This work was supported by the Visionary Imaging Services, Inc (Japan).

# Author

Tatsuaki Kobayashi 

# Release and updation

2022/4/7 initial commit.

