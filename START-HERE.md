# Linkod Mobile — local service-booking demo

## Open on your computer

Requires Node.js 24.19.x. No npm installation step or API key is needed.

1. Extract the ZIP and open the `linkod-mobile-demo` folder.
2. Double-click `start-demo.bat` on Windows, or run `npm run demo` in this folder.
3. Open http://127.0.0.1:3000. On desktop it appears in a phone-width frame.

## Open on your phone

1. Connect the phone and computer to the same trusted Wi-Fi.
2. Stop any running demo with Ctrl+C.
3. Double-click `start-mobile-demo.bat`, or run `npm run demo:lan`.
4. Open the printed `Phone demo` URL in your phone browser. Allow the local server through your computer firewall if needed.

The LAN launcher deliberately makes sample customer/provider/admin accounts available to devices on your network. Do not expose it to the public internet. Device GPS usually requires HTTPS or localhost, so a phone accessing a plain HTTP LAN address should use saved places or map pin selection instead. Browser GPS permission is only requested when you tap the location control.

## Demonstrate the journey

1. Tap the service location field. Type `Rizal`, `Manila`, `SM`, or `Makati` to see demo suggestions.
2. Select a suggestion, Home, Work, or a recent place. The map centres on that location.
3. Drag the pin, pan the map, or tap the map to choose an entrance, then confirm.
4. Choose Plumbing, Electrical, or Aircon Cleaning. Add a unit and access instructions if desired.
5. Tap Find a service provider. A real demo booking is saved in the local backend.
6. Tap Play provider demo. This uses the actual assigned demo offer, accepts it, and starts a 30-second simulated journey.
7. After simulated arrival, tap Start demo service, then Complete demo service.
8. Review the job in Activity. Saved places can be added separately from the Saved tab.

The simulation path is a straight dotted line, not a driving route. Its countdown is simulated, not a calculated road ETA. Navigating away stops simulated movement; reopening the job allows replay. Refresh status to see changes made from another workspace.

## Included

- Touch-first customer mobile web interface with real Leaflet street map.
- Local address autocomplete catalogue, saved Home/Work samples, recent places, and device GPS when browser permissions permit.
- Adjustable service pin; unit/building details and access notes.
- Actual API booking creation, history, cancellation, offer acceptance, and lifecycle transitions.
- Explicitly labelled provider journey simulation; no background or live GPS tracking.
- Saved places on the current device and separate local backend booking storage.
- Existing provider/admin workspace retained under `/workspace`, linked from Account. Provider pages remain the earlier responsive workspace, not a finished driver-style mobile app.

## Data and connections

Street tiles require internet and load directly from OpenStreetMap with visible attribution. No map tiles are bundled or prefetched. Leaflet 1.9.4 is bundled under its BSD-2-Clause license in `public/LEAFLET-LICENSE.txt`.

Address suggestions are a finite local demo catalogue with approximate sample pins. They are not Google Places, live geocoding, or verified residential addresses. Moving a pin does not reverse-geocode a new street address; the UI marks it as adjusted. Type an address into the search and use Choose on map to set a location outside the catalogue.

Prices and provider profiles are sample values. No payment is collected. Calling/chat, real ratings, inspection quotations, background GPS, navigation routing, production authentication, provider onboarding and native Android/iOS packaging are not included. This is a mobile web demonstration, not an APK or App Store build.

The demo launcher refreshes sample providers when started and gives them all three service skills. Provider GPS expires after ten minutes. If bookings expire, use the provider workspace's Go online in Manila control before booking again. Each offer expires after two minutes. Data persists in `data/linkod-mobile-demo.db` between runs. Files in that directory are generated locally and are not included in this distribution.

## Development

- `public/mobile.js`, `public/mobile.css`, `public/index.html`: new customer experience.
- `public/workspace.*`: retained provider/admin/customer dashboard.
- `src/demo.js`: local and optional LAN launchers.
- `src/app.js`: explicit static asset routes and existing API.
- `npm test`: backend test suite.

Production remains disabled for demo identities. The earlier framework review's scaling, payment, authentication and dispatch eligibility improvements remain future work.

## Verification for this delivery

All 16 existing backend tests passed. Automated Chromium checks at phone sizes covered address suggestions, pin confirmation, booking with access notes, saved places, provider acceptance, simulated travel, service completion, and a 320px-wide layout with no horizontal overflow or JavaScript errors. External map requests were blocked during automated testing; live tile availability and real-device GPS have not been verified.
