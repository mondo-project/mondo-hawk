notes regarding versions:

2.0.1 -- stable and most efficient version to use for Hawk

2.1.6 -- stable but less efficient in database manipulation (inserts/updates) and similar querying times

2.2.0 -- UNSTABLE -- cannot use DERIVED_ATTRIBUTE indexes at all as the keyset is limited to 128 (uses: java.util.concurrent.atomic.AtomicInteger) 
						and derived attribute indexes commonly use a lot more than 128 keys -- so it crashes.