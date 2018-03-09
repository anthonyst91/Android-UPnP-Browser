Android UPnP Library
==============================

Forked from "Android UPnP Browser" (available on the [Play Store][1])

A simple and Open Source UPnP library for Android. It retrieves the local UPnP devices and their informations and dynamically add them to your RecyclerView.Adapter

Usage
------------------------------

UPnPHelper is the main entry. You only need to provide it your own adapter (which should extends UPnPDeviceAdapter)

'''android
	UPnPDeviceAdapter<MyViewHolder> adapter = new UPnPDeviceAdapter<MyViewHolder>(this){
		@NonNull
		@Override
		public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//CREATE YOUR HOLDER
		}

		@Override
		public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
			//BIND YOUR HOLDER
		}
		
		@Override
		@DrawableRes
		public int getDefaultIcon() {
			//RETURN DEFAULT DEVICE DRAWABLE
		}
	};
	
	UPnPHelper helper = new UPnPHelper(adapter);
	helper.startObserver();
'''

The UPnPHelper will open a socket and wait for the UPnPDevices. Each time a new UPnPDevice is found, it will added to your adapter. If the device found already exists in the adapter, it will automatically updated if its parameters have changed.

You can change the default UPnP socket timeout (the default value is set to 60 seconds)

'''android
	UPnPHelper helper = new UPnPHelper(adapter, 10000); // 10 secondes
	helper.startObserver();
'''

You can stop the observation process at any moment by calling destroyObserver.

'''android
    helper.destroyObserver();
'''


You can override UPnPHelper in order to execute your own code in the following callbacks:

'''android
	/**
	 * Called when the first device has been found, before adding it to the adapter.
	 */
    void onFirstUPnPDeviceFound();
    
    /**
	 * Called each time a device is found / updated.
	 */
    void onUPnPDeviceFound(@NonNull UPnPDevice device);
    
    /**
	 * Called when the UPnP observation has ended (timeout - or destroyObserver called)
	 */
    void onUPnPObserverEnded();
    
    /**
	 * Called on UPnP observation error.
	 */
    void onUPnPObserverError();
'''


Applications that use the lib
------------------------------

[Printoid for OctoPrint LITE](https://play.google.com/store/apps/details?id=fr.yochi76.printoid.phones.trial&utm_source=github&utm_medium=upnplibrary)

[Printoid for OctoPrint PRO](https://play.google.com/store/apps/details?id=fr.yochi76.printoid.phones.pro&utm_source=github&utm_medium=upnplibrary)

[Printoid for OctoPrint PREMIUM](https://play.google.com/store/apps/details?id=fr.yochi76.printoid.phones.premium&utm_source=github&utm_medium=upnplibrary)

 [1]: https://play.google.com/store/apps/details?id=com.dgmltn.upnpbrowser
