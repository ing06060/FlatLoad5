package com.example.flatload

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.activity_input_way.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

//import com.mapbox.search.MapboxSearchSdk


class InputWayActivity : AppCompatActivity() { //출발지 도착지 입력 화면 - gps 허용 추가

    private val TAG = "DirectionActivity"
    val LOAD_SUCCESS = 101
    private val currentRoute: DirectionsRoute? = null
    private val client: MapboxDirections? = null

    var origin = Point.fromLngLat(0.0,0.0)
    var destination = Point.fromLngLat(0.0, 0.0)


    //private var textviewJSONText: TextView? = null

    var fusedLocationClient: FusedLocationProviderClient?= null
    var loc= LatLng(0.0,0.0)
    var startLoc= LatLng(0.0,0.0)
    var endLoc= LatLng(0.0,0.0)

    var locationCallback: LocationCallback?=null
    var locationRequest: LocationRequest?=null
    var mStartResultLocation = listOf<Address>()
    var mEndResultLocation = listOf<Address>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_way)
        init()
    }

    private fun init() {
        //처음에 gps 허용
        //내위치 누르면 현재 위치로 설정
        //확인 버튼 누르면 출발지, 도착지 -> 위도 경도로 변경 -> json 객
        // 체 만들기 -> MapActivity

        val mgeocorder: Geocoder = Geocoder(this, Locale.getDefault())
        //val getPointFromGeoCoder("서울특별시 송파구 방이동 112-1");
        initLocation() //gps 설정

        button3.setOnClickListener { //내 위치 버튼
            //loc.latitude,loc.longitude <- 현재 위치 위도 경도
            //위도 경도-> 텍스트 변경해서 출발지 edittext에 표시
            val txtLoc = mgeocorder.getFromLocation(loc.latitude,loc.longitude,1)[0]
            editTextStart.setText(txtLoc.getAddressLine(0))
            Log.i("my location", txtLoc.toString())

        }
        button.setOnClickListener { //확인 버튼
            //출발지 도착지 텍스트 위도 경도 변경 -> json 객체 만들기 -> 서버에 전달
            val start = editTextStart.text.toString()
            val end = editTextEnd.text.toString()

            if(start.isNotEmpty() && end.isNotEmpty()) {

                mStartResultLocation = mgeocorder.getFromLocationName(start, 1)
                mEndResultLocation = mgeocorder.getFromLocationName(end, 1)

                if(mStartResultLocation.isNotEmpty() && mEndResultLocation.isNotEmpty()){
                //if(origin != emptyPoint && destination != emptyPoint){ //지오코딩 함수가 리턴되면

                    val startLat = mStartResultLocation.get(0).latitude
                    val startLng = mStartResultLocation.get(0).longitude
                    startLoc = LatLng(startLat, startLng)

                    val endLat = mEndResultLocation.get(0).latitude
                    val endLng = mEndResultLocation.get(0).longitude
                    endLoc = LatLng(endLat, endLng)

                    Log.i("start location", startLoc.toString())
                    Log.i("end location", endLoc.toString())

                    val lat = startLoc.latitude
                    val log = startLoc.longitude

                    origin = Point.fromLngLat(startLoc.longitude,startLoc.latitude) //출발 좌표 포인트
                    destination = Point.fromLngLat(endLoc.longitude,endLoc.latitude) //목적 좌표 포인트


                    Log.i("start point", origin.toString())
                    Log.i("end point", destination.toString())

                    getRoute(origin, destination)

                    //JSONTask(startLoc,endLoc).execute("http://192.168.0.8:3000/post") //로컬 서버로 좌표 보내기
                    //mapactivity 이동
                    /*
                    val i = Intent(this,MapActivity::class.java)
                    startActivity(i)*/

                }else{
                    Toast.makeText(this,"위치를 다시 입력해주세요", Toast.LENGTH_LONG).show()
                    editTextStart.text.clear()
                    editTextEnd.text.clear()
                }

            }else{
                Toast.makeText(this,"위치를 입력해주세요", Toast.LENGTH_LONG).show()
                editTextStart.text.clear()
                editTextEnd.text.clear()
            }
        }
    }

    private fun getRoute(origin: Point, destination: Point) {
        //변수 선언
        //var getrouteSteps = <Steps>()
        //var getrouteSteps = Steps()

        var pairList = mutableListOf<Pair<Double,Double>>()

        //맵박스 길찾기 요청
        val client = MapboxDirections.builder() //builder 패턴 방식으로 MapboxDirections 클래스의 객체룰 생성. 변수의 순서 바뀌면 안됨
            .origin(origin) //출발지
            .destination(destination) //목적지
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_WALKING) //교통, 운전, 걷기, 사이클링
            .steps(true)
            //.geometries("geojson")
            .accessToken(getString(R.string.access_token))
            .build()

        //길찾기 응답
        client?.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {

                if (response.body() == null) {
                    Log.i("error", "No routes found, make sure you set the right user and access token.")
                    return
                } else if (response.body()!!.routes().size < 1) {
                    Log.i("error", "No routes found")
                    return
                }

                // Get the directions route
                val currentRoute = response.body()!!.routes()[0]

                //textviewJSONText?.setText(response.body()!!.toJson())

                val jsonString = response.body()!!.toJson().trimIndent()//json 형식으로 바꿔서 string에 저장
                val jsonObject = JSONObject(jsonString)
                val jsonArray = jsonObject.getJSONArray("routes")
                val subjsonObject = jsonArray.getJSONObject(0) //route 배열의 index = 0
                val subjsonArray = subjsonObject.getJSONArray("legs")
                val subjsonObject2 = subjsonArray.getJSONObject(0)//legs 배열의 index = 0
                val subjsonArray2 = subjsonObject2.getJSONArray("steps")

                var cnt:Int = 0

                //json 파싱 intersection
                for( i in 0..subjsonArray2.length()-1){ //step배열의 index 0 부터 끝까지
                    val iObject = subjsonArray2.getJSONObject(i) //index i 의 값을 객체로 생성
                    val iArray =iObject.getJSONArray("intersections") //intersection 배열

                    for (j in 0..iArray.length()-1){
                        val jObject =iArray.getJSONObject(j)
                        val location =jObject.getJSONArray("location") //intersection 배열의 location 값을 얻어옴

                        println("${i+1}번째 intersections ${j+1}번째 location"+location)

                        val pair = Pair(location[0].toString().toDouble(), location[1].toString().toDouble())
                        pairList.add(cnt, pair)
                        cnt = cnt + 1
                    }
                }

                var a: String = ""
                for(i in 0..pairList.size-1){
                    a = a + pairList.get(i).toString() + "\n"
                }

                textviewJSONText?.setText(a) //textview로 띄움


            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Log.i("error", "Error: " + throwable.message)

            }
        }
        )

    }

    /*
    //서버로 좌표 보내는 AsynTask
    public class JSONTask(startLoc: LatLng, endLoc: LatLng) : AsyncTask<String?, String?, String?>() {

        val startLoc = startLoc //출발 좌표
        val endLoc = endLoc //도착 좌표

        override fun doInBackground(vararg params: String?): String? {
            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                val jsonObject = JSONObject()
                jsonObject.accumulate("start_location",startLoc.toString())
                jsonObject.accumulate("end_location", endLoc.toString() )

                var con: HttpURLConnection? = null
                var reader: BufferedReader? = null
                try {
                    //URL url = new URL("http://192.168.25.16:3000/users");
                    val url = URL(params[0])
                    //연결을 함
                    con = url.openConnection() as HttpURLConnection
                    con.requestMethod = "POST" //POST방식으로 보냄
                    con!!.setRequestProperty("Cache-Control", "no-cache") //캐시 설정
                    con.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    ) //application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "text/html") //서버에 response 데이터를 html로 받음
                    con.doOutput = true //Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.doInput = true //Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect()

                    //서버로 보내기위해서 스트림 만듬
                    val outStream = con.outputStream
                    //버퍼를 생성하고 넣음
                    val writer =
                        BufferedWriter(OutputStreamWriter(outStream))
                    writer.write(jsonObject.toString())
                    writer.flush()
                    writer.close() //버퍼를 받아줌

                    //서버로 부터 데이터를 받음
                    val stream = con.inputStream
                    reader = BufferedReader(InputStreamReader(stream))
                    val buffer = StringBuffer()
                    var line: String? = ""
                    while (reader.readLine().also { line = it } != null) {
                        buffer.append(line)
                    }
                    return buffer.toString() //서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    con?.disconnect()
                    try {
                        reader?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
             //서버로 부터 받은 값을 출력해주는 부분
            Log.i("After send to server", result)
        }

    }*/
    private fun startLocationUpdates() { //gps 관련
        locationRequest = LocationRequest.create()?.apply {
            interval= 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                //성공적으로 위치정보 업데이트 되었으면? 그 위치 정보 가져옴
                locationResult ?: return
                for(location in locationResult.locations){
                    loc= LatLng(location.latitude,location.longitude)
                    Log.i("changeLocation",loc.toString())
                }
                // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,16.0f))
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback, //갱신되면 이함수 호출
            Looper.getMainLooper()) //메인쓰레드가 가지고있는 루퍼 객체 사용하겠다*/
    }

    private fun initLocation() {
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
        {
            getuserlocation() //현재위치 갱신
            startLocationUpdates() //업데이트
        }
        else{
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),100)
            //처음엔 권한 요청함
        }
    }

    private fun getuserlocation() {
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient?.lastLocation?.addOnSuccessListener {//성공적으로 위치 가져왔으면?
            loc = LatLng(it.latitude,it.longitude) //현재위치로 위치정보를 바꾸겠다
            Log.i("currentLocation",loc.toString())

        }
    }

    override fun onRequestPermissionsResult( //권한요청하고 결과 여기로 옴
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==100){ //허용받았으면
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED){ //둘다 허용되면
                getuserlocation()
                startLocationUpdates()
            }
            else{//허용안해줬으면 기본 맵으로
                Toast.makeText(this,"위치정보 제공을 하셔야 합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

