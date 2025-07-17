# Tantusz Operator – AI-ügynök specifikáció (v0.1)

## 📌 Áttekintés
A `Tantusz Operator` egy **állapot-alapú, többlépcsős döntéshozatalra képes AI-ügynök**, amely a Pannonia-DAO karbonelszámolási rendszerét, kézbesítési logikáit és Tantusz tokenképzést automatizálja. Támogatja a futárokat, ügyfélszolgálatot, riportálást és a szabályalapú DAO governance workflow-kat.

---

## 🧠 Képességek

- 🔄 **Kézbesítési állapotfigyelés** és Tantusz-képzés
- 📈 **CO2 kalkuláció** súly, távolság, járműtípus alapján
- 📝 **Jegyzőkönyvek és panaszok automatikus kezelése** (ÁSZF szerint)
- 🧾 **DAO governance workflow-k előkészítése**
- 🌐 **Webes adatlekérdezés / API integráció** (pl. időjárás, árfolyam, címellenőrzés)
- 🔁 **Memória / naplókezelés** és döntéshozatal több változóból

---

## ⚙️ Input struktúrák

### 1. `delivery_event.json`
```json
{
  "timestamp": "2025-07-17T12:43:00Z",
  "package_id": "MP-20250717-00874",
  "status": "damaged",
  "weight_kg": 1.7,
  "delivery_distance_km": 14.2,
  "vehicle_type": "motorbike",
  "recipient_refused": true,
  "photo_attached": true,
  "new_address": null
}
```

### 2. `tantusz_policy.yaml`
```yaml
base_fee_huf: 1490
co2_base_kg_per_km: 0.092
co2_factor_per_kg: 1.1
inflation_protection: true
exchange:
  HUF: 1
  EUR: 0.0025
  BTC: 0.00000058
```

---

## 🧮 Tantusz kalkulációs logika

```
CO2_kibocsátás = táv_km × co2_base × tömeg_kg × co2_factor
Tantusz_érték = 1 csomag = 1 Tantusz (→ fix egység, de háttérértékkel)
CO2_többlet = Tantusz_karbon_score → + governance fee (ha átlépi küszöböt)
```

---

## 👠 Agent funkciók

| Modul | Leírás |
|-------|--------|
| `calculate_co2()` | Visszaadja egy kézbesítés CO2 kibocsátását |
| `generate_token_receipt()` | Tantusz nyugta generálása (PDF / JSON) |
| `escalate_dispute()` | Panaszkezelési eljárás elindítása ÁSZF szerint |
| `suggest_routing()` | Új kézbesítési cím javaslat (AI térképi input alapján) |
| `update_dao_ledger()` | Tantusz és karbon metaadat bejegyzés DAO-n belül |

---

## 🛡️ Integrációs pontok

- 🚀 **REST API Gateway**
- 📂 **GitHub (smart contract frissítés)**
- 📦 **Kézbesítő Asszisztens LogHub (CSV / JSON)**
- 📈 **BI / Carbon Score Dashboard**

---

## 🤭 Jövőbeli fejlesztések

- [ ] LLM auditlog auto-ellenőrzés (fairness, compliance)
- [ ] Lokális LLM fallback ha nincs hálózat
- [ ] Haptikus futárasszisztens (wearable AI agent)
- [ ] Tantusz–karbon marketplace integráció (DEX)

---

## 🧑‍🚀 Ügynök személyisége / interakciós stílusa

- **Név:** Tantusz Operator
- **Hangnem:** Katonás, száraz humorral
- **Stílus:** Célorientált, hatékony, de barátságos
- **Célja:** A kézbesítés és karbonlábnyom szimultán optimalizálása, a DAO irányelveinek megfelelően

---

## 📁 Fájlstruktúra (ajánlott)

```
tantusz_operator/
├— operator_config.yaml
├— delivery_event.json
├— tantusz_policy.yaml
├— logs/
│   └— 2025-07-17-delivery_log.json
├— functions/
│   ├— calculate_co2.py
│   ├— escalate_dispute.py
│   └— generate_token_receipt.py
└— reports/
    └— MP-20250717-00874_token_receipt.pdf
