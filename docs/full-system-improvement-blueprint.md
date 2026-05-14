# Alumni Connect: Full System Improvement Blueprint

## 1) Alumni Profile Standard (Single Source of Truth)

### Academic fields
- `batchYear`
- `graduationYear`
- `classSection`
- `rollNumber`
- `stream`
- `specialization`
- `higherEducation` (or `university` + `degree`)

### Employment fields
- `jobTitle`
- `company`
- `industry`
- `businessOwner`
- `businessName`
- `skills`

### Social + Contact fields
- `linkedin`, `twitter`, `github`
- `instagram`, `facebook`, `personalWebsite`
- `phoneNumber`, `whatsappNumber`
- `currentCity`, `currentCountry`, `hometown`

### Identity + Trust
- `verified` flag + verified badge in UI
- `bio`
- `profilePhotoUrl`, `coverPhotoUrl`

### Profile quality score
- Keep `getProfileCompletion()` and show `%` in dashboard + profile header.

---

## 2) Role Capability Matrix (Strict)

### Member / Alumni
1. View only own batch member directory
2. View batch notices
3. View approved event posts
4. View school + batch gallery
5. Donate to school/batch campaigns
6. Edit own profile

### Batch Controller (CR)
1. Keeps all Member capabilities
2. Verify batch members (if school permission enabled)
3. Approve/reject batch member requests (if enabled)
4. Add alumni to assigned batch (if enabled)
5. Create/manage batch notices (if enabled)
6. Create event posts
7. Approve batch posts (if enabled)
8. Upload gallery content (if enabled)
9. Organize batch reunions/activities

### School Admin
1. Create/manage multiple batches
2. Approve/reject member registrations
3. Assign/remove CR
4. Moderate school content/posts
5. Manage gallery/announcements
6. Edit school profile
7. Verify alumni identities
8. Configure CR permissions
9. Suspend/delete members of own school only

### Software Owner
1. Add schools
2. Add/manage school admins
3. Activate/deactivate schools
4. Monitor cross-school activity
5. Platform analytics
6. Platform-wide feature toggles
7. Partnership approval

---

## 3) Security & Permission Rules (Must Enforce)

1. **School boundary checks** on every admin action.
2. **Batch boundary checks** on every CR action.
3. CR users must also have member access (`ROLE_MEMBER` authority mapping for CR is correct).
4. Suspended users cannot login (`accountNonLocked = !suspended`).
5. Replace GET logout links with **POST `/logout` + CSRF** forms (implemented).

---

## 4) UX Upgrades for Professional Look

1. Keep single app shell (left sidebar + mobile offcanvas) on all dashboards.
2. Use consistent card spacing, heading scale, and button heights (`44px` touch minimum).
3. Add status chips everywhere: `Active`, `Pending`, `Suspended`, `Verified`.
4. For large tables, add search + filters + pagination.
5. For mobile, convert heavy tables to stacked cards.

---

## 5) High-Impact Next Implementation Sprints

### Sprint A (Governance)
- Activity logs for all privileged actions
- Hard ownership checks for school/batch actions
- CR permission gates for notice/post/gallery/member moderation

### Sprint B (Trust)
- Identity verification workflow + badge
- Suspended/deleted member audit trail
- Duplicate account detection (email/phone + school)

### Sprint C (Engagement)
- Better notifications (type filter + unread counter in nav)
- Realtime chat (polling/WebSocket)
- Event RSVP capacity + attendee export

### Sprint D (Scale)
- CSV import validation report (row-wise success/error)
- Async processing for large imports
- Analytics caching + dashboard performance tuning

---

## 6) Acceptance Checklist

- [ ] Member cannot access other batch data
- [ ] CR cannot act outside own batch
- [ ] School admin cannot manage another school
- [ ] Owner can see platform-wide analytics and controls
- [ ] Logout works from every dashboard
- [ ] Suspended user cannot authenticate
- [ ] Verified badge visible in member directory and profile
