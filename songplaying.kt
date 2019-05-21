package com.example.echo.fragments


import android.accounts.OnAccountsUpdateListener
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import com.example.echo.CurrentSongHelper
import com.example.echo.R
import com.example.echo.Songs
import com.example.echo.databases.EchoDatabase
import com.example.echo.fragments.SettingsFragment.Satisfied.MY_PREFS_NAME
import com.example.echo.fragments.SongPlayingFragment.Staticated.onSongComplete
import com.example.echo.fragments.SongPlayingFragment.Staticated.playNext
import com.example.echo.fragments.SongPlayingFragment.Staticated.updateTextViews
import com.example.echo.fragments.SongPlayingFragment.Satisfied.audioVisualization
import com.example.echo.fragments.SongPlayingFragment.Satisfied.currentPosition
import com.example.echo.fragments.SongPlayingFragment.Satisfied.endTimeText
import com.example.echo.fragments.SongPlayingFragment.Satisfied.fab
import com.example.echo.fragments.SongPlayingFragment.Satisfied.favoriteContent
import com.example.echo.fragments.SongPlayingFragment.Satisfied.fetchSongs
import com.example.echo.fragments.SongPlayingFragment.Satisfied.glView
import com.example.echo.fragments.SongPlayingFragment.Satisfied.loopImageButton
import com.example.echo.fragments.SongPlayingFragment.Satisfied.currentSongHelper
import com.example.echo.fragments.SongPlayingFragment.Satisfied.mediaPlayer
import com.example.echo.fragments.SongPlayingFragment.Satisfied.myActivity
import com.example.echo.fragments.SongPlayingFragment.Satisfied.nextImageButton
import com.example.echo.fragments.SongPlayingFragment.Satisfied.playpauseImageButton
import com.example.echo.fragments.SongPlayingFragment.Satisfied.previousImageButton
import com.example.echo.fragments.SongPlayingFragment.Satisfied.seekBar
import com.example.echo.fragments.SongPlayingFragment.Satisfied.shuffleImageButton
import com.example.echo.fragments.SongPlayingFragment.Satisfied.songArtistView
import com.example.echo.fragments.SongPlayingFragment.Satisfied.songTitleView
import com.example.echo.fragments.SongPlayingFragment.Satisfied.startTimeText
import com.example.echo.fragments.SongPlayingFragment.Satisfied.updateSongTime
import com.example.echo.fragments.SongPlayingFragment.Staticated.MY_PREFS_LOOP
import com.example.echo.fragments.SongPlayingFragment.Staticated.MY_PREFS_SHUFFLE
import com.example.echo.fragments.SongPlayingFragment.Staticated.processInformation
import kotlinx.android.synthetic.main.fragment_song_playing.*
import kotlinx.android.synthetic.main.fragment_song_playing.view.*
import org.w3c.dom.Text
import java.sql.Time
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 *
 */
class SongPlayingFragment : Fragment() {
    object Satisfied {
        var myActivity: Activity? = null
        var mediaPlayer: MediaPlayer? = null
        var startTimeText: TextView? = null
        var endTimeText: TextView? = null
        var playpauseImageButton: ImageButton? = null
        var previousImageButton: ImageButton? = null
        var nextImageButton: ImageButton? = null
        var loopImageButton: ImageButton? = null
        var seekBar: SeekBar? = null
        var songArtistView: TextView? = null
        var songTitleView: TextView? = null
        var shuffleImageButton: ImageButton? = null
        var currentPosition: Int = 0
        var checkSize: Int = 0

        var fetchSongs: ArrayList<Songs>? = null
        var currentSongHelper: CurrentSongHelper? = null
        var audioVisualization: AudioVisualization? = null
        var glView: GLAudioVisualizationView? = null
        var fab: ImageButton? = null
        var favoriteContent: EchoDatabase? = null
        var mSensorManager: SensorManager? = null
        var mSensorListener: SensorEventListener? = null
        var updateSongTime = object : Runnable {
            override fun run() {
                val getcurrent = mediaPlayer?.currentPosition
                startTimeText?.setText(
                    String.format(
                        "%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(getcurrent?.toLong() as Long),
                        TimeUnit.MILLISECONDS.toSeconds(getcurrent?.toLong()) -
                                TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getcurrent?.toLong() as Long))
                    )
                )
                seekBar?.setProgress((getcurrent?.toInt() as Int))
                Handler().postDelayed(this, 1000)
            }
        }
    }

    object Staticated {
        var MY_PREFS_SHUFFLE = "Shuffle feature"
        var MY_PREFS_LOOP = "Loop feature"
        fun onSongComplete() {
            if (Satisfied.currentSongHelper?.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
                Satisfied.currentSongHelper?.isPlaying = true
            } else {
                if (Satisfied.currentSongHelper?.isPlaying as Boolean) {
                    Satisfied.currentSongHelper?.isPlaying = true
                    var nextSong = Satisfied.fetchSongs?.get(Satisfied.currentPosition)
                    Satisfied.currentSongHelper?.songTitle = nextSong?.songTitle
                    Satisfied.currentSongHelper?.songPath = nextSong?.songData
                    Satisfied.currentSongHelper?.songArtist = nextSong?.songArtist
                    Satisfied.currentSongHelper?.currentPosition = Satisfied.currentPosition
                    Satisfied.currentSongHelper?.songId = nextSong?.songID as Long
                    updateTextViews(
                        Satisfied.currentSongHelper?.songTitle as String,
                        Satisfied.currentSongHelper?.songArtist as String
                    )

                    mediaPlayer?.reset()
                    try {
                        mediaPlayer?.setDataSource(
                            myActivity, Uri.parse(Satisfied.currentSongHelper?.songPath)
                        )
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                        processInformation(Satisfied.mediaPlayer as MediaPlayer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    playNext("PlayNextNormal")
                    currentSongHelper?.isPlaying = true
                }
            }
            if (favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                fab?.setBackgroundResource(R.drawable.favorite_on)
            } else {
                Satisfied.fab?.setBackgroundResource(
                    R.drawable.favorite_off
                )
            }
        }

        fun updateTextViews(songtitle: String, songArtist: String) {
            Satisfied.songTitleView?.setText(songtitle)
            Satisfied.songArtistView?.setText(songArtist)

        }

        fun processInformation(mediaPlayer: MediaPlayer) {
            var finalTime = mediaPlayer.duration
            var startTime = mediaPlayer.currentPosition
            seekBar?.max = finalTime
            Satisfied.startTimeText?.setText(
                String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime?.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime?.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime?.toLong()))
                )
            )
            Satisfied.endTimeText?.setText(
                String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime?.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime?.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime?.toLong()))
                )
            )
            seekBar?.setProgress(startTime)
            Handler().postDelayed(Satisfied.updateSongTime, 1000)
        }

        fun playNext(check: String) {
            if (check.equals("PlayNextNormal", true)) {
                Satisfied.currentPosition = Satisfied.currentPosition + 1
            } else if (check.equals("PlayNextLikeNormalShufle", true)) {
                var randomObject = Random()
                var randomPosition = randomObject.nextInt(Satisfied.fetchSongs?.size?.plus(1) as Int)
                Satisfied.currentPosition = randomPosition
            }
            if (Satisfied.currentPosition == Satisfied.fetchSongs?.size) {
                Satisfied.currentPosition = 0
            }
            currentSongHelper?.isLoop = false
            var nextSong = Satisfied.fetchSongs?.get(Satisfied.currentPosition)
            Satisfied.currentSongHelper?.songTitle = nextSong?.songTitle
            Satisfied.currentSongHelper?.songPath = nextSong?.songData
            Satisfied.currentSongHelper?.currentPosition = Satisfied.currentPosition
            Satisfied.currentSongHelper?.songId = nextSong?.songID as Long
            updateTextViews(
                Satisfied.currentSongHelper?.songTitle as String,
                Satisfied.currentSongHelper?.songArtist as String
            )

            Satisfied.mediaPlayer?.reset()
            try {
                Satisfied.mediaPlayer?.setDataSource(
                    Satisfied.myActivity,
                    Uri.parse(Satisfied.currentSongHelper?.songPath)
                )
                Satisfied.mediaPlayer?.prepare()
                Satisfied.mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (Satisfied.favoriteContent?.checkifIdExists(Satisfied.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                Satisfied.fab?.setBackgroundResource(R.drawable.favorite_on)
            } else {
                Satisfied.fab?.setBackgroundResource(R.drawable.favorite_off)
            }
        }
    }

    var mAcceleration: Float = 0f
    var mAccelerationCurrent: Float = 0f
    var mAccelerationLast: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_song_playing, container, false)
        setHasOptionsMenu(true)
        Satisfied.seekBar = view?.findViewById(R.id.seekBar)
        startTimeText = view?.findViewById(R.id.startTime)
        endTimeText = view?.findViewById(R.id.endTime)
        playpauseImageButton = view?.findViewById(R.id.playPauseButton)
        nextImageButton = view?.findViewById(R.id.nextButton)
        previousImageButton = view?.findViewById(R.id.previousButton)
        loopImageButton = view?.findViewById(R.id.loopButton)
        shuffleImageButton = view?.findViewById(R.id.shuffleButton)
        songArtistView = view?.findViewById(R.id.songArtist)
        songTitleView = view?.findViewById(R.id.songTitle)
        glView = view?.findViewById(R.id.favoriteIcon)
        fab = view?.findViewById(R.id.favoriteIcon)
        fab?.alpha = 0.8f
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Satisfied.audioVisualization = Satisfied.glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Satisfied.myActivity = activity
    }

    override fun onResume() {
        super.onResume()
        Satisfied.audioVisualization?.onResume()
        Satisfied.mSensorManager?.registerListener(
            Satisfied.mSensorListener, Satisfied.mSensorManager?.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ),
            SensorManager.SENSOR_DELAY_NORMAL
        )

    }

    override fun onPause() {
        Satisfied.audioVisualization?.onPause()
        super.onPause()
        Satisfied.mSensorManager?.unregisterListener(Satisfied.mSensorListener)
    }

    override fun onDestroyView() {
        Satisfied.audioVisualization?.release()
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Satisfied.mSensorManager = Satisfied.myActivity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAcceleration = 0.0f
        mAccelerationCurrent = SensorManager.GRAVITY_EARTH
        mAccelerationLast = SensorManager.GRAVITY_EARTH
        bindShakeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.song_playing_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        val item:MenuItem?= menu?.findItem(R.id.action_redirect)
        item?.isVisible = true
        val item2:MenuItem?= menu?.findItem(R.id.action_sort)
        item2?.isVisible = false


    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.action_redirect->{
                Satisfied.myActivity?.onBackPressed()
                return false
            }
        }
        return false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        favoriteContent = EchoDatabase(myActivity)

        SongPlayingFragment.Satisfied.currentSongHelper = CurrentSongHelper()
        SongPlayingFragment.Satisfied.currentSongHelper?.isPlaying = true
        currentSongHelper?.isLoop = false
        currentSongHelper?.isShuffle = false
        var path: String? = null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var songId: Long = 0
        try {
            path = arguments?.getString("path")
            _songTitle = arguments?.getString("songTitle")
            _songArtist = arguments?.getString("songArtist")
            songId = arguments?.getInt("songId")!!.toLong()
            currentPosition = arguments!!.getInt("songPosition")
            fetchSongs = arguments?.getParcelableArrayList("songData")
            currentSongHelper?.songPath = path
            currentSongHelper?.songTitle = _songTitle
            currentSongHelper?.songArtist = _songArtist
            currentSongHelper?.songId = songId
            currentSongHelper?.currentPosition = currentPosition
            updateTextViews(
                Satisfied.currentSongHelper?.songTitle as String,
                Satisfied.currentSongHelper?.songArtist as String
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var fromFavBottomBar = arguments?.get("FavBottomBar") as? String
         var fromMainScreenBottomBar = arguments!!.getString("MainScreeBottomBar")
        if (fromFavBottomBar != null) {
            mediaPlayer = Satisfied.mediaPlayer
        } else {
            mediaPlayer = MediaPlayer()
            Satisfied.mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            try {

                mediaPlayer?.setDataSource(myActivity, Uri.parse(path))
                mediaPlayer?.prepare()

            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer?.start()
        }
        processInformation(mediaPlayer as MediaPlayer)
        if (Satisfied.currentSongHelper?.isPlaying as Boolean) {
            Satisfied.playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            Satisfied.playpauseImageButton?.setBackgroundResource(R.drawable.play_icon)

        }
        Satisfied.mediaPlayer?.setOnCompletionListener {
            onSongComplete()
        }
        clickHandler()
        var visualizationHandler = DbmHandler.Factory.newVisualizerHandler(myActivity as Context, 0)
        Satisfied.audioVisualization?.linkTo(visualizationHandler)
        var prefsForShuffle =
            myActivity?.getSharedPreferences(MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)

        var isShuffleAllowed = prefsForShuffle?.getBoolean("feature", false)
        if (isShuffleAllowed as Boolean) {
            Satisfied.currentSongHelper?.isShuffle = true
            Satisfied.currentSongHelper?.isLoop = false
            Satisfied.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            Satisfied.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
        } else {
            Satisfied.currentSongHelper?.isShuffle = false
            Satisfied.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
        }
        var prefsForLoop =
            myActivity?.getSharedPreferences(MY_PREFS_LOOP, Context.MODE_PRIVATE)
        var isLoopAllowed = prefsForLoop?.getBoolean("feature", false)
        if (isLoopAllowed as Boolean) {
            currentSongHelper?.isShuffle = false
            currentSongHelper?.isLoop = true
            shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            loopImageButton?.setBackgroundResource(R.drawable.loop_icon)

        } else {
            loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            currentSongHelper?.isLoop = false
        }
        if (favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            fab?.setBackgroundResource(R.drawable.favorite_on)
        } else {
            fab?.setBackgroundResource(R.drawable.favorite_off)
        }
    }

    fun clickHandler() {
        Satisfied.fab?.setOnClickListener({
            if (Satisfied.favoriteContent?.checkifIdExists(Satisfied.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                Satisfied.fab?.setImageResource(R.drawable.favorite_off)
                Satisfied.favoriteContent?.deleteFavourite(Satisfied.currentSongHelper?.songId?.toInt() as Int)
                Toast.makeText(myActivity, "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                fab?.setImageResource(R.drawable.favorite_on)
                favoriteContent?.storeAsFavorite(
                    currentSongHelper?.songId?.toInt(),
                    currentSongHelper?.songArtist,
                    currentSongHelper?.songTitle,
                    currentSongHelper?.songPath
                )
                Toast.makeText(myActivity, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        })
        shuffleImageButton?.setOnClickListener({
            var editorShuffle =
                myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)
                    ?.edit()
            var editorLoop =
                myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()
            if (currentSongHelper?.isShuffle as Boolean) {
                Satisfied.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                Satisfied.currentSongHelper?.isShuffle = false
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()

            } else {
                Satisfied.currentSongHelper?.isShuffle = true
                Satisfied.currentSongHelper?.isLoop = false
                Satisfied.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
                Satisfied.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()

            }

        })
        nextImageButton?.setOnClickListener({
            Satisfied.currentSongHelper?.isPlaying = true
            Satisfied.playpauseImageButton
            if (currentSongHelper?.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
            } else {
                playNext("PlayNextNormal")
            }
        })
        previousImageButton?.setOnClickListener({
            Satisfied.currentSongHelper?.isPlaying = true
            if (Satisfied.currentSongHelper?.isLoop as Boolean) {
                Satisfied.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)

            }
            playPrevious()
        })
        loopImageButton?.setOnClickListener({
            var editorShuffle = Satisfied.myActivity?.getSharedPreferences(
                Staticated.MY_PREFS_SHUFFLE,
                Context.MODE_PRIVATE
            )?.edit()
            var editorLoop =
                Satisfied.myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)
                    ?.edit()

            if (Satisfied.currentSongHelper?.isLoop as Boolean) {
                Satisfied.currentSongHelper?.isLoop = false
                Satisfied.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)

            } else {
                Satisfied.currentSongHelper?.isLoop = true
                Satisfied.currentSongHelper?.isShuffle = false
                Satisfied.loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
                Satisfied.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()
            }
        })
        playpauseImageButton?.setOnClickListener({
            if (Satisfied.mediaPlayer?.isPlaying as Boolean) {
                Satisfied.mediaPlayer?.pause()
                currentSongHelper?.isPlaying = false
                Satisfied.playpauseImageButton?.setBackgroundResource(R.drawable.play_icon)
            } else {
                Satisfied.mediaPlayer?.start()
                currentSongHelper?.isPlaying = true
                Satisfied.playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }


    fun playPrevious() {
        Satisfied.currentPosition = Satisfied.currentPosition - 1
        if (Satisfied.currentPosition == -1) {
            Satisfied.currentPosition = 0
        }
        if (Satisfied.currentSongHelper?.isPlaying as Boolean) {
            Satisfied.playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            Satisfied.playpauseImageButton?.setBackgroundResource((R.drawable.play_icon))
        }
        var nextSong = fetchSongs?.get(Satisfied.currentPosition)
        Satisfied.currentSongHelper?.songTitle = nextSong?.songTitle
        Satisfied.currentSongHelper?.songPath = nextSong?.songData
        Satisfied.currentSongHelper?.currentPosition = Satisfied.currentPosition
        Satisfied.currentSongHelper?.songId = nextSong?.songID as Long
        updateTextViews(
            Satisfied.currentSongHelper?.songTitle as String,
            Satisfied.currentSongHelper?.songArtist as String
        )

        Satisfied.mediaPlayer?.reset()
        try {
            Satisfied.mediaPlayer?.setDataSource(activity, Uri.parse(Satisfied.currentSongHelper?.songPath))
            Satisfied.mediaPlayer?.prepare()
            Satisfied.mediaPlayer?.start()
            processInformation(Satisfied.mediaPlayer as MediaPlayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (Satisfied.favoriteContent?.checkifIdExists(Satisfied.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            fab?.setBackgroundResource(R.drawable.favorite_on)
        } else {
            fab?.setBackgroundResource(R.drawable.favorite_off)
        }
    }

    fun bindShakeListener() {
        Satisfied.mSensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent?) {
                val x = event!!.values[0]
                val y = event.values[1]
                val z = event.values[2]
                mAccelerationLast = mAccelerationCurrent
                mAccelerationCurrent = Math.sqrt(((x * x + y * y + z * z).toDouble())).toFloat()
                val delta = mAccelerationCurrent - mAccelerationLast
                mAcceleration = mAcceleration * 0.9f + delta
                if (mAcceleration > 12) {
                    val prefs = Satisfied.myActivity?.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
                    val isAllowed = prefs?.getBoolean("feature", false)
                    if (isAllowed as Boolean) {
                        Staticated.playNext("PlayNextNormal")
                    }
                }
            }
        }
    }
}