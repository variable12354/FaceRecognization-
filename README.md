This Demo Tesing Usecase

in this demo previously auto added drable images

![image](https://github.com/variable12354/FaceRecognization-/assets/136910752/f011ecdd-5eaa-411c-b5a6-9ec3a3646e1d)

currently you have testing with this predefine images if you want to test your side 
  1. replace drawable images with your images and try to detect face ( if stored face is detect in current camera face you can get result in main activity and display name tag -> in this tag you can add the person name and id setup).
  2. like In dynamic way you get lots of employee images url from api at this point you can get a bitmap from uri. 
   
    private fun loadImageFromUrl(imageUrl: String): Bitmap? {
         return   try {
             val url = URL(imageUrl)
             val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
             connection.doInput = true
             connection.connect()

             val inputStream: InputStream = connection.inputStream
             val bitmap = BitmapFactory.decodeStream(inputStream)
             bitmap
         } catch (e: Exception) {
             e.printStackTrace()
             null
         }
    }

How to find accurate result 

detected face is verified or avalible in data or not with using K-Nearest Neighbor(KNN) Algorithm.
![ql_cecb1b0a3631a3bbfe9714eb2732b231_l3](https://github.com/variable12354/FaceRecognization-/assets/136910752/c9e4a5f7-e07c-4676-a8fe-476886725bc9)

      
      private fun findNearestFace(vector: FloatArray): Pair<String, Float>? {
            var ret: Pair<String, Float>? = null
            for (person in recognisedFaceList) {
                val name: String = person.name!!
                val knownVector: FloatArray = person.faceVector!!
                var distance = 0f
                for (i in vector.indices) {
                    val diff = vector[i] - knownVector[i]
                    distance += diff * diff
                }
                Log.e(TAG, "findNearestFace:distance :$distance")
                distance = sqrt(distance.toDouble()).toFloat()
                if (ret == null || distance < ret.second) {
                    Log.e(TAG, "retention:$ret ")
                    ret = Pair(name, distance)
                }
            }
            Log.e(TAG, "findNearestFace Result $ret")
            return ret
    }

Accuricy is the depend on result in above function return float value. 
    as per result if value > 1.0f so face is note match with store images (person is not verified).
    as per result if value < 1.0f so face is match with store images (person is verified).


  
