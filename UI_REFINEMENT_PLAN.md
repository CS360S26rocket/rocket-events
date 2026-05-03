# UI Refinement Plan

## Goal
Refine the Rocket Events Android app into a polished, Paymo-inspired campus events product while preserving every existing feature and Firebase flow already present in `integration-m2`.

## Design Direction
- Keep the existing dark visual identity, but make it more premium with deeper surfaces, clearer contrast, softer borders, and consistent spacing.
- Use the shared lime accent for primary actions and purple only for secondary/payment-oriented actions.
- Use rounded, glass-like cards inspired by the supplied Figma screens and Paymo-style mobile finance surfaces.
- Improve readability by tightening typography hierarchy: clear page titles, compact helper text, strong button labels, and scannable cards.
- Make important workflows feel complete: role selection, login, discovery, event details, RSVP/ticketing, organizer dashboard, event creation, proof review, wishlist, and map.

## Functional Guardrails
- Do not remove existing features.
- Keep current Firebase repositories, activity names, IDs, and navigation flows compatible with existing Java code.
- Avoid changing collection names or data contracts unless required for a bug fix.
- Keep layouts native Android XML and Java-based to match the project.

## Planned Refinements
- Shared theme:
  - Add a richer color system for elevated surfaces, muted surfaces, warning chips, success chips, and Paymo-style purple action states.
  - Update shared card/button/input drawables so all screens benefit without rewriting every activity.
  - Add reusable icon-button, bottom-navigation, status-chip, and hero-media drawables.
- Role and auth:
  - Make role selection feel closer to the Figma first screen with stronger account cards and clearer labels.
  - Polish campus/organizer login/register screens with elevated auth panels and consistent button/input treatment.
- Campus user flow:
  - Upgrade Campus Home to look like a modern discovery feed with better event cards, category/filter controls, selected event detail, ticket tier selection, and clearer status messages.
  - Preserve RSVP, paid-ticket selection, proof/status, wishlist, map, and notification-related entry points.
  - Make event detail information more scannable: price, capacity, venue, date/time, payment QR state.
- Organizer flow:
  - Refine Organizer Dashboard cards, event selector, attendee output, and action buttons.
  - Keep create event, attendee list, proof review, QR upload, close RSVP, and broadcast actions available.
  - Polish Create Event, Ticket Setup, and Info Setup wizard screens to match the Figma stepper style.
- Payment and proof screens:
  - Keep M2/M4 upload/review functionality intact.
  - Improve visual hierarchy for QR upload, pending proofs, and proof viewer screens through shared styling.
- Reliability improvements:
  - Keep Gradle compile passing after each refinement pass.
  - Check for missing IDs after XML edits.
  - Avoid UI text truncation in compact buttons by increasing button widths or shortening labels where needed.

## Implemented In Current Pass
- Shared UI polish:
  - Updated the main dark color system, elevated card styling, input fields, primary buttons, secondary buttons, role cards, event cards, and ticket cards.
  - Added reusable drawables for icon buttons, ghost buttons, warning actions, success/warning chips, bottom navigation, and media-style event headers.
- Role/auth/dashboard polish:
  - Refined role selection, campus login, organizer login, organizer dashboard, create event, ticket setup, and info setup screens to better match the supplied dark Figma direction.
  - Fixed cramped ticket setup controls by widening the Edit/Remove controls and moving them onto compact icon-style surfaces.
- Campus event discovery:
  - Replaced plain text event rows with richer discovery cards built dynamically in `CampusHomeActivity`.
  - Event cards now show a media-style header, date badge, event title, venue/category metadata, category chip, and price/free chip.
  - Tapping an event now loads a clearer selected-event detail state instead of only storing an ID silently.
- Paymo-inspired ticketing:
  - Paid events now show selectable ticket tiers with price and availability.
  - Added quantity controls for paid ticket selection.
  - The RSVP action now switches between `RSVP Free` and `Pay with Paymo` based on event type.
  - Paid selection now creates a paid order through the existing ticket repository path and tells the user to upload payment proof next.
- Organizer portal flow:
  - Added a loading screen before role selection, using the app name as the first screen.
  - Rebuilt the organizer side around a persistent four-item bottom navbar: Dashboard, Events, Payments, Profile.
  - Dashboard now shows event stats, a Create Event action, and a list of organizer events.
  - Event Management lists existing events and opens a dedicated edit screen for the selected event.
  - Payment Verification lists events first; tapping an event opens the existing proof review screen for that event's screenshots.
  - Profile shows organizer account details and sign out.
  - Added a dedicated Edit Event screen for title, date/time, venue, capacity, description, save, and cancel-event actions.
  - Edit Event now includes a Send Update action that opens a separate organizer update screen for date, time, venue, and custom attendee messages.
  - Organizer event updates are sent through the existing notifications collection and appear in the campus user's Notifications tab.
- Campus user portal flow:
  - Rebuilt the campus side around a separate four-item bottom navbar: Home, My Events, Notifications, Profile.
  - Home keeps search, date filter, price filter, and active event discovery.
  - Event rows are now visual event cards inspired by the supplied Ticketly screenshots.
  - Tapping an event opens a dedicated Event Details screen with description, date/venue/capacity, ticket tiers, quantity, register, and payment proof upload.
  - My Events now renders registered events as cards and opens the same detail screen.
  - Notifications now reads in-app notification documents for the campus user and displays organizer/event updates.
  - Profile follows the supplied dark Paymo-style profile structure without a My Bookings section.

## Verification Checklist
- Run `./gradlew --no-daemon --offline :app:compileDebugJavaWithJavac`. Completed successfully after the current pass.
- Manually verify:
  - Role selection opens campus and organizer flows.
  - Campus login opens home.
  - Discovery list loads and event cards are tappable.
  - Free RSVP still works.
  - Paid ticket tier selection still works.
  - Organizer login opens dashboard.
  - Create Event wizard still passes data across all three steps.
  - Payment QR upload and pending proof review screens still open.
  - Wishlist and map screens still open.
