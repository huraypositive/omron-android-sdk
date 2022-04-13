package net.huray.omronsdk.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.huray.omronsdk.OmronDeviceManager
import net.huray.omronsdk.ble.entity.DiscoveredDevice
import net.huray.omronsdk.ble.entity.WeightDeviceInfo
import net.huray.omronsdk.ble.enumerate.OHQCompletionReason
import net.huray.omronsdk.ble.enumerate.OHQSessionType
import net.huray.omronsdk.ble.enumerate.OmronDeviceType
import net.huray.omronsdk.model.DeviceConnectionState
import net.huray.omronsdk.utils.Const
import net.huray.omronsdk.utils.PrefUtils

class DeviceRegisterViewModel(
    private val omronDeviceType: OmronDeviceType
) : ViewModel(), OmronDeviceManager.RegisterListener {

    private val _connectionEvent = MutableLiveData<DeviceConnectionState>()
    val connectionEvent: LiveData<DeviceConnectionState> get() = _connectionEvent

    private val _loadingEvent = MutableLiveData<Boolean>()
    val loadingEvent: LiveData<Boolean> get() = _loadingEvent

    private val omronManager: OmronDeviceManager = OmronDeviceManager(
        omronDeviceType.omronDeviceCategory,
        OHQSessionType.REGISTER,
        this
    )

    private var userIndex = 0
    private var deviceAddress: String? = null

    init {
        startScan()
    }

    override fun onCleared() {
        super.onCleared()
        omronManager.stopScan()
    }

    override fun onScanned(discoveredDevices: MutableList<DiscoveredDevice>?) {
        if (discoveredDevices == null) return

        _connectionEvent.postValue(
            DeviceConnectionState.OnScanned(discoveredDevices)
        )
    }

    override fun onRegisterFailed(reason: OHQCompletionReason?) {
        setLoadingState(false)

        if (reason == null) return

        _connectionEvent.postValue(DeviceConnectionState.ConnectionFailed(reason))
    }

    override fun onRegisterSuccess() {
        setLoadingState(false)
        completeRegister()
    }

    fun startScan() {
        if (omronManager.isScanning) {
            omronManager.stopScan()
        }

        omronManager.startScan()
    }

    fun cancel() {
        omronManager.cancelSession()
    }

    fun connectDevice(deviceAddress: String) {
        setLoadingState(true)
        _connectionEvent.value = DeviceConnectionState.Connecting

        this.deviceAddress = deviceAddress

        if (omronDeviceType.isHBF222F) {
            connectWeightDevice(deviceAddress)
            return
        }

        if (omronDeviceType.is9200T) {
            omronManager.connectBpDevice(deviceAddress)
            return
        }
    }

    private fun connectWeightDevice(deviceAddress: String) {
        val deviceInfo = WeightDeviceInfo.newInstanceForRegister(
            Const.demoUser,  // This should be real user data in product code
            deviceAddress,
            userIndex
        )

        omronManager.connectWeightDevice(deviceInfo)
    }

    private fun completeRegister() {
        requireNotNull(deviceAddress) { "deviceAddress is null" }

        _connectionEvent.postValue(DeviceConnectionState.ConnectionSuccess)

        if (omronDeviceType.isHBF222F) {
            PrefUtils.saveBodyCompositionMonitorHbf222tAddress(deviceAddress!!)
            PrefUtils.saveBodyCompositionMonitorHbf222tUserIndex(userIndex)
            return
        }

        if (omronDeviceType.is9200T) {
            PrefUtils.saveBpMonitorHem9200tDeviceAddress(deviceAddress)
            return
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        _loadingEvent.postValue(isLoading)
    }
}