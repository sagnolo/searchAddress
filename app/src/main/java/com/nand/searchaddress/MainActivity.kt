package com.nand.searchaddress

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.*
import com.google.android.material.internal.ViewUtils.dpToPx
import com.nand.searchaddress.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_address.*
import kotlinx.android.synthetic.main.item_address.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainActivity : AppCompatActivity() {

    var page = 1
    lateinit var dialog: AlertDialog
    lateinit var nativeAd: TemplateView
    lateinit var retrofit: Retrofit
    lateinit var binding: ActivityMainBinding
    lateinit var interstitialAd: InterstitialAd
    lateinit var endlessRecyclerViewScrollListener: EndlessRecyclerViewScrollListener
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.main = this

        retrofit = RetrofitClient.getInstance()

        MobileAds.initialize(this, getString(R.string.google_ads_app_id))
        val adRequest = AdRequest.Builder().build()

        interstitialAd = InterstitialAd(this).apply {
            adUnitId = getString(R.string.google_ads_interstitial_id)
            loadAd(adRequest)
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    interstitialAd.loadAd(AdRequest.Builder().build())
                }
            }
        }
        binding.adView.loadAd(adRequest)

        setDialogAd()

        binding.rvAddress.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL, false)
        endlessRecyclerViewScrollListener = object : EndlessRecyclerViewScrollListener(binding.rvAddress.layoutManager!!) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                searchAddress(true)
            }
        }
        binding.rvAddress.addOnScrollListener(endlessRecyclerViewScrollListener)
        binding.rvAddress.adapter = AddressAdapter(arrayListOf()).apply{
            setOnItemClickListener{ adapter, i ->
                var itemList = (adapter as AddressAdapter).items
                val view = View.inflate(this@MainActivity, R.layout.dialog_address, null)

                val okButton = view.findViewById<Button>(R.id.btn_ok)
                val jibunText = view.findViewById<TextView>(R.id.txt_jibun_address)
                val roadText = view.findViewById<TextView>(R.id.txt_road_address)
                val postalText = view.findViewById<TextView>(R.id.txt_postal)
                val jibunLayout = view.findViewById<LinearLayout>(R.id.layout_jibun)
                val postalLayout = view.findViewById<LinearLayout>(R.id.layout_postal)
                val roadLayout = view.findViewById<LinearLayout>(R.id.layout_road)
                val dialog = AlertDialog.Builder(this@MainActivity).setView(view).setCancelable(true).create()
                okButton.setOnClickListener {
                    dialog.dismiss()
                }
                roadText.setText(itemList[i].roadAddr)
                jibunText.setText(itemList[i].jibunAddr)
                postalText.setText(itemList[i].zipNo)
                var clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                jibunLayout.setOnClickListener {
                    val clip : ClipData = ClipData.newPlainText("jibunAddress",jibunText.text.toString())
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "지번 주소가 복사되었습니다", Toast.LENGTH_SHORT).show()
                }
                postalLayout.setOnClickListener {
                    val clip : ClipData = ClipData.newPlainText("postalCode", postalText.text.toString())
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "우편번호가 복사되었습니다", Toast.LENGTH_SHORT).show()
                }
                roadLayout.setOnClickListener {
                    val clip : ClipData = ClipData.newPlainText("roadAddress", roadText.text.toString())
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "도로명 주소가 복사되었습니다", Toast.LENGTH_SHORT).show()
                }
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                dialog.show()

            }
        }
        binding.btnSearch.setOnClickListener {
            page=1
            count++
            if(count % 3 == 0)
                showAd()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.editAddress.windowToken, 0)
            searchAddress(false)
        }
        binding.editAddress.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                page=1
                count++
                if(count % 3 == 0)
                    showAd()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(binding.editAddress.windowToken, 0)
                searchAddress(false)
            }
            true
        }
        binding.editAddress.requestFocus()
    }

    fun showAd(){
        if (interstitialAd.isLoaded)
            interstitialAd.show()
    }

    fun setDialogAd(){
        val view = View.inflate(this@MainActivity, R.layout.dialog_exit, null)
        val txtExit = view.findViewById<TextView>(R.id.txt_exit)
        nativeAd = view.findViewById<TemplateView>(R.id.ad_template)
        val adRequest = AdRequest.Builder().build()
        CoroutineScope(Dispatchers.Main).launch {
            Handler().postDelayed({
                var builder = AdLoader.Builder(applicationContext, getString(R.string.google_ads_native_id))
                    .forUnifiedNativeAd {
                        nativeAd.setNativeAd(it)
                    }
                var adLoader = builder.build()
                adLoader.loadAd(adRequest)
            },500)
        }
        txtExit.setOnClickListener {
            finish()
        }
        dialog = AlertDialog.Builder(this@MainActivity).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss()
                finish()
                true
            }
            false
        }
    }

    fun searchAddress(nextSearch: Boolean){
        val retroService = retrofit.create(RetroService::class.java)
        val call = retroService.getAddressInfo(
            "U01TX0FVVEgyMDIxMDYwOTE0MTg0OTExMTI2MzU=",
            page++,
            100,
            binding.editAddress.text.toString(),
            "json"
        )
        call.enqueue(object: Callback<Address>{
            override fun onResponse(call: Call<Address>, response: Response<Address>) {
                if(response.isSuccessful){
                    var address = response.body()
                    var results = address?.results
                    var juso = results?.juso
                    val adapter = binding.rvAddress.adapter as AddressAdapter
                    if(!nextSearch){
                        adapter.items.clear()
                        if (juso != null) {
                            adapter.items.addAll(juso)
                        }
                        if (juso == null) Toast.makeText(applicationContext, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        else if(juso.size == 0) Toast.makeText(applicationContext, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    } else {
                        val oldCount = adapter.items.size
                        if (juso != null ) {
                            adapter.items.addAll(juso)
                            adapter.notifyItemRangeInserted(oldCount + 1, juso?.size)
                        }
                    }
                    if(juso != null)
                        if (juso!!.size > 0)
                            adapter.setShowFooterProgress(true)
                        else adapter.setShowFooterProgress(false)
                }
            }

            override fun onFailure(call: Call<Address>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    override fun onBackPressed() {
        if(!dialog.isShowing){
            dialog.show()
        }
    }

    class AddressAdapter(val items: ArrayList<Address.Results.Juso>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        private val VIEW_TYPE_ITEM = 0
        private val VIEW_TYPE_LOADING = 1
        private var showFooterProgress = false
        private var onItemClickListener: OnItemClickListener? = null

        fun setOnItemClickListener(listener: (RecyclerView.Adapter<*>, Int) -> Unit) {
            onItemClickListener = object : OnItemClickListener {
                override fun onItemClick(adapter: RecyclerView.Adapter<*>, position: Int) {
                    listener(adapter, position)
                }
            }
        }

        fun setShowFooterProgress(show: Boolean) {
            if (showFooterProgress == show) return
            showFooterProgress = show
            if (items.size > 0) notifyItemChanged(items.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when(viewType){
                VIEW_TYPE_ITEM -> ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false))
                else -> ProgressViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_progress, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if(holder is ItemViewHolder){
                holder.bind(items[position])
                holder.itemView.setOnClickListener {
                    onItemClickListener?.onItemClick(this@AddressAdapter, position)
                }
            } else if (holder is ProgressViewHolder) {
                if (showFooterProgress)
                    holder.itemView.setVisibility(View.VISIBLE)
                else
                    holder.itemView.setVisibility(View.GONE)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position >= items.size) 1 else 0
        }

        override fun getItemCount(): Int {
            val size = items.size
            return if (size == 0) 0 else size + 1
        }

        class ProgressViewHolder(itemView:View): RecyclerView.ViewHolder(itemView)

        class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            fun bind(item: Address.Results.Juso){
                itemView.txt_road_address.setText(item.roadAddr)
                itemView.txt_jibun_address.setText(item.jibunAddr)
                itemView.txt_post.setText(item.zipNo)
            }
        }

        interface OnItemClickListener {
            fun onItemClick(adapter: RecyclerView.Adapter<*>, position: Int)
        }
    }
}