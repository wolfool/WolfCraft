# WolfCraft

커스텀 제작 시스템 for Minecraft Paper 1.21+

## Features
- 🔨 커스텀 레시피 (설정 파일로 자유 추가)
- ⏰ 시간 기반 제작 대기열 (실시간 진행바)
- 📋 카테고리별 GUI (무기/방어구/도구/음식/특수)
- ✨ 인챈트된 아이템 결과물
- 📊 재료 현황 실시간 표시 (보유/필요)
- 🎮 경험치 소비 시스템
- 🔒 퍼미션 기반 레시피 잠금

## Commands
| 명령어 | 설명 |
|---|---|
| `/craft` (`/제작`) | 제작 GUI 열기 |
| `/craft queue` | 제작 대기열 확인 |
| `/craft <레시피>` | 바로 제작 시작 |
| `/craftadmin reload` | 설정 리로드 |

## 기본 레시피
| 레시피 | 재료 | 시간 |
|---|---|---|
| 강화된 다이아 검 | 다이아5 + 블레이즈막대2 + 철3 | 30초 |
| 강화 철 갑옷 | 철12 + 다이아2 + 가죽4 | 45초 |
| 초월 곡괭이 | 다이아3 + 금5 + 막대2 | 60초 |
| 황금 만찬 | 금사과1 + 스테이크8 + 금당근4 | 20초 |
| 토템 파편 | 에메랄드16 + 다이아8 + 네더별1 + 금블럭4 | 120초 |

## 커스텀 레시피 추가
config.yml에서 쉽게 추가:
```yaml
recipes:
  my_recipe:
    display: "<red>나만의 레시피</red>"
    category: 무기
    ingredients:
      DIAMOND: 3
    result:
      material: DIAMOND_SWORD
      name: "<red>커스텀 검</red>"
      enchantments:
        SHARPNESS: 5
    time: 30
    exp-cost: 10
```
