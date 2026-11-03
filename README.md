# HyperDesk 

![Android Version](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![LSPosed Framework](https://img.shields.io/badge/LSPosed-Required-blue)
![HyperOS](https://img.shields.io/badge/HyperOS-Supported-orange)
![License](https://img.shields.io/badge/License-MIT-green.svg)

An advanced Xposed/Vector module designed to bring ultimate icon customization to the HyperOS Launcher. Break free from default icons and apply any custom third-party icon pack directly to your system launcher.

> **⚠️ Developer Note:**
> Developer Notes: I was tinkering with my Redmi Note 12 Topaz (a piece of junk) and wanted to create something that would allow me to customize my device without using Nova.
> I came across this interesting project—originally by ukmcoder, and forked by egorovsankin988-create.
> I modified the hooks to make it compatible with HyperOS 2. Added icon pack mixing, Added the ability to customize system icons, Refined the notification badges, Plus other minor improvements.

## 📸 Screenshots

<p align="center">
  <b>HyperOS with Custom Icon Packs</b>
  <br><br>
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/d5e4b912-fced-4dc9-be8a-a7644807909b" />
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/7eda9262-90bc-4238-8ad2-10a4625ef273" />
</p>

<br>

<p align="center">
  <b>Module Interface</b>
  <br><br>
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/1871b5ff-173a-4215-aa51-11a8714af0e5" />
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/d43a2968-022d-4f89-96dd-adfc5f3a5051" />
  <br>
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/c8bdf9ce-3461-40e9-b9ba-77f37c1bbe42" />
  <img width="auto" height="600" alt="image" src="https://github.com/user-attachments/assets/28f96d75-019e-48e8-a3b1-0212ad88635d" /> 
</p>

## ✨ HyperDesk 2.0

HyperDesk is the successor of IconsCustomizer: one LSPosed module for the HyperOS 3+ desktop **and** System UI on Xiaomi, Redmi and POCO devices.

| Category | What you can change |
| --- | --- |
| Icons | Icon packs, fallback packs and per-app icons from any pack (mix packs freely), per-app names, pinned shortcuts such as Cleaner, frosted notification badges and opened folder blur with adjustable levels, uniform size across packs, Monet recolor, icon size (relative, stays centred), shadows |
| Labels | Hide app / dock names, label text size |
| Dock | 9 materials (stock, glass, frosted with real blur, liquid glass, solid, Monet, gradient, AMOLED, outline), tint, opacity, border, corner radius, width, padding, unlimited icons |
| Folders | Same materials, corner radius, opacity, border, folder columns |
| Widgets | Monet clock widget colors |
| Recents & blur | Card corner radius, full recents blur, hide status bar, launcher blur strength |
| Animations | Presets (Snappy → Cinematic), app open/close duration, recents transition, full / high-end animations, system animation scales (root) |
| Control center | Corner radius of toggle tiles, cards and sliders |
| Volume panel | Corner radius of the panel and slider, panel blur |

Every category has a **live preview**, changes are collected and applied with one tap (**Apply**), and settings can be **exported / imported** as JSON. Items marked **Beta** use hooks that vary between ROM builds; if one has no effect, the LSPosed log shows `Hook '<name>' unavailable`.

**LSPosed scope:** System Launcher (`com.miui.home`) and/or POCO Launcher (`com.mi.android.globallauncher`), plus **System UI** (`com.android.systemui`) for control center and volume panel tweaks.

## ✨ Features

* **Universal Icon Pack Support:** Apply almost any standard Android icon pack from the Play Store directly to the stock HyperOS launcher.
* **Per-App Customization:** Change icons on an individual app basis without affecting the rest of your system.
* **Seamless Integration:** Works cleanly with the native launcher without requiring bulky third-party home screen replacements.
* **Dynamic Sizing:** Adjust icon dimensions globally for a perfect fit on your display.

## ⚠️ Prerequisites

Before installing, ensure your device meets the following requirements:
1. **Root Access:** Magisk or KernelSU installed.
2. **Xposed Framework:** Vector (Zygisk-Next or ReZygisk) installed and active.
3. **OS:** HyperOS 2.0 or newer (Android 15+) on Xiaomi, Redmi and POCO devices. Supports both the HyperOS System Launcher (`com.miui.home`) and the POCO Launcher (`com.mi.android.globallauncher`). Originally tested on Poco F7 (HyperOS 3.0/3.1 China launcher).
4. Apply Default Icons From HyperOS Themes App For Best Results.

## 🚀 Installation

1. Download the latest `.apk` from the [Releases](../../releases) page.
2. Install the APK on your device.
3. Open the **Vector**.
4. Go to the **Modules** tab and enable **HyperDesk**.
5. Ensure your launcher is checked in the scope list: **System Launcher** (`com.miui.home`) and/or **POCO Launcher** (`com.mi.android.globallauncher`). The app's status card shows which launcher your device uses.
6. **Force Stop Launcher App Or Reboot your device** to apply the hooks.

## 🛠️ How to Use

1. Open the **HyperDesk** app from your app drawer.
2. Enable the **Allow Custom Icon Packs** option.
3. Select your desired installed icon pack from the list.
4. If some apps have missing icons in the icon pack, use the **"Apply Custom Icon to Specific Apps"** option and choose an identical icon from the icon pack for that app.
5. Tap the **"Restart launcher"** button to apply changes immediately if they are not automatically applied (uses root when available).
6. **"Restart System UI"** (root) restarts the status bar, notifications and navigation without rebooting the phone.

> **🎨 Note on Monet Theming:**
> If you are using icon packs with Monet theming colors, you will need to use the **ColorBlendr** app to manage your Monet color palette control. You must click the **Restart Launcher** button every time you change your wallpaper or make changes to your Monet color palette to apply those new colors to your icons.
> It is recommended to use **Override Icon Colors** option only with Icon Packs That Support's dynamic Monet Wallpaper based colors.

## 📜 License

This project is licensed under the [MIT License](LICENSE).
