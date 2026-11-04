package dev.vmellos.airhockey.core.audio

import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sintetizador procedural puro de áudio PCM para Air Hockey.
 * Gera trilhas musicais (BGM) e efeitos sonoros (SFX) matematicamente para todos os 11 temas,
 * sem depender de arquivos de áudio estáticos pesados no APK.
 */
object ProceduralAudioSynthesizer {

    private const val SAMPLE_RATE = 22050

    /**
     * Gera o arquivo BGM temático em loop para a arena correspondente.
     */
    fun generateBgmWav(file: File, theme: TableTheme) {
        val (bpm, totalBars) = when (theme) {
            TableTheme.CYBER_SOCCER -> Pair(128.0, 8)
            TableTheme.NEO_BASKETBALL -> Pair(96.0, 8)
            TableTheme.GRAVITY_VOLLEY -> Pair(135.0, 8)
            TableTheme.GRIDIRON_FOOTBALL -> Pair(110.0, 8)
            TableTheme.CYBER_BASEBALL -> Pair(120.0, 8)
            TableTheme.WASTELAND_APOCALYPSE -> Pair(132.0, 8)
            TableTheme.RETRO_SYNTHWAVE -> Pair(118.0, 8)
            TableTheme.TOKAMAK_REACTOR -> Pair(134.0, 8)
            TableTheme.CYBER_PONG -> Pair(120.0, 8)
            TableTheme.CYBER_COLOSSEUM -> Pair(114.0, 8)
            TableTheme.CLASSIC_NEON -> Pair(125.0, 8)
        }

        val beatDuration = 60.0 / bpm
        val beatsPerBar = 4
        val totalDuration = totalBars * beatsPerBar * beatDuration
        val numSamples = (SAMPLE_RATE * totalDuration).toInt()
        val pcmData = ByteArray(numSamples * 2)

        var noiseSeed = 987654321L
        fun pseudoNoise(): Double {
            noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFF
            return (noiseSeed.toDouble() / 0x7FFFFFFF) * 2.0 - 1.0
        }

        var phaseBass = 0.0
        var phaseLead = 0.0
        var phasePad = 0.0
        var phaseKick = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val barIndex = ((t / (4.0 * beatDuration)).toInt()).coerceIn(0, totalBars - 1)
            val subBar = barIndex % 4
            val tBeat = t % beatDuration
            val beatIndex = (t / beatDuration).toInt()

            var sample = 0.0

            when (theme) {
                TableTheme.CLASSIC_NEON -> {
                    // Synthwave Arcade A-minor (Am, F, C, G)
                    val roots = doubleArrayOf(110.0, 87.31, 130.81, 98.00)
                    val fifths = doubleArrayOf(164.81, 130.81, 196.00, 146.83)
                    val octaves = doubleArrayOf(220.0, 174.61, 261.63, 196.00)
                    val root = roots[subBar]

                    // Kick four-on-floor
                    if (tBeat < 0.20) {
                        val env = exp(-24.0 * tBeat)
                        phaseKick += 2.0 * PI * (48.0 + 110.0 * exp(-36.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * env * 0.42
                    }
                    // Snare nos tempos 2 e 4
                    if (beatIndex % 2 == 1 && tBeat < 0.16) {
                        val env = exp(-20.0 * tBeat)
                        sample += (pseudoNoise() * 0.6 + sin(2.0 * PI * 185.0 * tBeat) * 0.4) * env * 0.26
                    }
                    // Hi-hat nos contratempos
                    val tEighth = (t + beatDuration * 0.5) % beatDuration
                    if (tEighth < 0.05) {
                        sample += pseudoNoise() * exp(-60.0 * tEighth) * 0.10
                    }
                    // Bass rolling arpeggio 16th
                    val sixteenthDuration = beatDuration / 4.0
                    val sIdx = ((t / sixteenthDuration).toInt()) % 4
                    val bFreq = when (sIdx) { 0, 1 -> root; 2 -> octaves[subBar]; else -> fifths[subBar] }
                    phaseBass += 2.0 * PI * bFreq / SAMPLE_RATE
                    val bassEnv = exp(-12.0 * (t % sixteenthDuration))
                    sample += (sin(phaseBass) + 0.3 * sin(phaseBass * 2.0)) * bassEnv * 0.28
                    // Lead melody
                    val melody = doubleArrayOf(440.0, 523.25, 659.25, 783.99, 880.0, 659.25, 587.33, 523.25)
                    val mFreq = melody[((t / (beatDuration / 2.0)).toInt()) % melody.size]
                    phaseLead += 2.0 * PI * mFreq / SAMPLE_RATE
                    sample += (sin(phaseLead) + 0.2 * sin(phaseLead * 3.0)) * exp(-10.0 * (t % (beatDuration / 2.0))) * 0.14
                }

                TableTheme.CYBER_SOCCER -> {
                    // Euro-Dance / Stadium House D-minor (Dm, Bb, F, C)
                    val roots = doubleArrayOf(146.83, 116.54, 174.61, 130.81)
                    val root = roots[subBar]

                    // Punchy house kick
                    if (tBeat < 0.18) {
                        val env = exp(-28.0 * tBeat)
                        phaseKick += 2.0 * PI * (55.0 + 130.0 * exp(-40.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * env * 0.45
                    }
                    // Offbeat upbeat bass (syncopated house bass)
                    val tOffbeat = (t + beatDuration * 0.5) % beatDuration
                    if (tOffbeat < beatDuration * 0.48) {
                        phaseBass += 2.0 * PI * (root * 0.5) / SAMPLE_RATE
                        val env = exp(-10.0 * tOffbeat)
                        sample += (sin(phaseBass) + 0.35 * sin(phaseBass * 2.0)) * env * 0.35
                    }
                    // Claps e caixa
                    if (beatIndex % 2 == 1 && tBeat < 0.14) {
                        sample += pseudoNoise() * exp(-24.0 * tBeat) * 0.24
                    }
                    // Whistle & upbeat synth riff
                    val leadNotes = doubleArrayOf(587.33, 698.46, 880.0, 783.99)
                    val leadFreq = leadNotes[((t / beatDuration).toInt()) % leadNotes.size]
                    phaseLead += 2.0 * PI * leadFreq / SAMPLE_RATE
                    sample += sin(phaseLead) * 0.12
                }

                TableTheme.NEO_BASKETBALL -> {
                    // Urban Trap / Hardwood 808 G-minor (Gm, Eb, F, D)
                    val roots = doubleArrayOf(98.0, 77.78, 87.31, 73.42)
                    val root = roots[subBar]

                    // Deep 808 sub kick
                    if (beatIndex % 2 == 0 && tBeat < 0.40) {
                        phaseKick += 2.0 * PI * (38.0 + 85.0 * exp(-18.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-7.0 * tBeat) * 0.50
                    }
                    // Snappy rimshot snare on 3
                    if (beatIndex % 4 == 2 && tBeat < 0.12) {
                        sample += (pseudoNoise() * 0.7 + sin(2.0 * PI * 340.0 * tBeat) * 0.3) * exp(-25.0 * tBeat) * 0.30
                    }
                    // Rapid trap hi-hats
                    val tHat = t % (beatDuration / 4.0)
                    if (tHat < 0.04) {
                        sample += pseudoNoise() * exp(-75.0 * tHat) * 0.12
                    }
                    // Funky synth lead
                    phaseLead += 2.0 * PI * (root * 4.0) / SAMPLE_RATE
                    sample += sin(phaseLead) * exp(-8.0 * tBeat) * 0.15
                }

                TableTheme.GRAVITY_VOLLEY -> {
                    // Orbital Space Chillstep E-minor (Em, C, D, Bm)
                    val roots = doubleArrayOf(164.81, 130.81, 146.83, 123.47)
                    val root = roots[subBar]

                    // Half-time soft deep kick
                    if (beatIndex % 4 == 0 && tBeat < 0.28) {
                        phaseKick += 2.0 * PI * (42.0 + 70.0 * exp(-20.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-12.0 * tBeat) * 0.38
                    }
                    // Soft ambient snare on beat 3
                    if (beatIndex % 4 == 2 && tBeat < 0.25) {
                        sample += pseudoNoise() * exp(-14.0 * tBeat) * 0.18
                    }
                    // Ethereal crystalline cosmic arpeggios
                    val celestialNotes = doubleArrayOf(659.25, 783.99, 987.77, 1174.66, 1318.51, 987.77)
                    val noteIdx = ((t / (beatDuration / 3.0)).toInt()) % celestialNotes.size
                    phaseLead += 2.0 * PI * celestialNotes[noteIdx] / SAMPLE_RATE
                    sample += (sin(phaseLead) + 0.3 * sin(phaseLead * 2.0)) * exp(-6.0 * (t % (beatDuration / 3.0))) * 0.16
                    // Warm space sub drone
                    phaseBass += 2.0 * PI * (root * 0.5) / SAMPLE_RATE
                    sample += sin(phaseBass) * 0.22
                }

                TableTheme.GRIDIRON_FOOTBALL -> {
                    // Industrial Stadium March B-minor (Bm, G, A, F#)
                    val roots = doubleArrayOf(123.47, 98.0, 110.0, 92.5)
                    val root = roots[subBar]

                    // Marching kick
                    if (tBeat < 0.22) {
                        phaseKick += 2.0 * PI * (50.0 + 90.0 * exp(-30.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-16.0 * tBeat) * 0.44
                    }
                    // Military snare rolls (triplets)
                    val tSnare = t % (beatDuration / 2.0)
                    if (tSnare < 0.10) {
                        sample += (pseudoNoise() * 0.8 + sin(2.0 * PI * 220.0 * tSnare) * 0.2) * exp(-22.0 * tSnare) * 0.22
                    }
                    // Heavy industrial buzzing saw bass
                    phaseBass += 2.0 * PI * (root * 0.5) / SAMPLE_RATE
                    val sawBass = ((phaseBass % (2.0 * PI)) / PI) - 1.0
                    sample += sawBass * 0.25
                    // Brass stadium fanfare chord
                    phaseLead += 2.0 * PI * (root * 2.0) / SAMPLE_RATE
                    sample += (sin(phaseLead) + 0.4 * sin(phaseLead * 1.5)) * 0.15
                }

                TableTheme.CYBER_BASEBALL -> {
                    // Ballpark Organ Electronic Swing C-Major (C, F, G, C)
                    val roots = doubleArrayOf(130.81, 174.61, 196.0, 130.81)
                    val root = roots[subBar]

                    // Swing kick
                    if ((beatIndex % 2 == 0) && tBeat < 0.20) {
                        phaseKick += 2.0 * PI * (52.0 + 100.0 * exp(-32.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-20.0 * tBeat) * 0.40
                    }
                    // Crisp baseball snare
                    if ((beatIndex % 2 == 1) && tBeat < 0.14) {
                        sample += pseudoNoise() * exp(-22.0 * tBeat) * 0.22
                    }
                    // Ballpark organ arpeggios
                    val organNotes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25)
                    val oIdx = ((t / (beatDuration / 3.0)).toInt()) % organNotes.size
                    phaseLead += 2.0 * PI * organNotes[oIdx] / SAMPLE_RATE
                    val organWave = sin(phaseLead) + 0.5 * sin(phaseLead * 2.0) + 0.25 * sin(phaseLead * 3.0)
                    sample += organWave * 0.18
                    // Walking bass
                    phaseBass += 2.0 * PI * root / SAMPLE_RATE
                    sample += sin(phaseBass) * 0.24
                }

                TableTheme.WASTELAND_APOCALYPSE -> {
                    // Distorted Post-Apocalyptic Industrial Darksynth F#-minor (F#m, D, E, C#)
                    val roots = doubleArrayOf(92.5, 73.42, 82.41, 69.3)
                    val root = roots[subBar]

                    // Heavy overdriven kick
                    if (tBeat < 0.24) {
                        phaseKick += 2.0 * PI * (44.0 + 140.0 * exp(-35.0 * tBeat)) / SAMPLE_RATE
                        val rawKick = sin(phaseKick) * exp(-14.0 * tBeat)
                        sample += (rawKick * 1.6).coerceIn(-1.0, 1.0) * 0.45
                    }
                    // Metallic anvil clatter snare
                    if (beatIndex % 2 == 1 && tBeat < 0.18) {
                        val anvil = sin(2.0 * PI * 1420.0 * tBeat) + sin(2.0 * PI * 2850.0 * tBeat) * 0.5
                        sample += (pseudoNoise() * 0.5 + anvil * 0.5) * exp(-18.0 * tBeat) * 0.32
                    }
                    // Grinding sawtooth bass (distorted)
                    phaseBass += 2.0 * PI * root / SAMPLE_RATE
                    val rawSaw = ((phaseBass % (2.0 * PI)) / PI) - 1.0
                    val distSaw = (rawSaw * 2.0).coerceIn(-1.0, 1.0)
                    sample += distSaw * 0.30
                    // Industrial siren
                    phaseLead += 2.0 * PI * (370.0 + 50.0 * sin(2.0 * PI * 2.0 * t)) / SAMPLE_RATE
                    sample += sin(phaseLead) * 0.10
                }

                TableTheme.RETRO_SYNTHWAVE -> {
                    // Outrun 80s Sunset D-minor (Dm, Bb, C, Am)
                    val roots = doubleArrayOf(146.83, 116.54, 130.81, 110.0)
                    val root = roots[subBar]

                    // Warm 80s kick
                    if (tBeat < 0.20) {
                        phaseKick += 2.0 * PI * (48.0 + 95.0 * exp(-30.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-18.0 * tBeat) * 0.40
                    }
                    // Gated reverb snare
                    if (beatIndex % 2 == 1 && tBeat < 0.22) {
                        sample += (pseudoNoise() * 0.65 + sin(2.0 * PI * 210.0 * tBeat) * 0.35) * exp(-14.0 * tBeat) * 0.28
                    }
                    // Driving octave bass
                    val eighthIdx = ((t / (beatDuration / 2.0)).toInt()) % 2
                    val bFreq = if (eighthIdx == 0) root * 0.5 else root
                    phaseBass += 2.0 * PI * bFreq / SAMPLE_RATE
                    sample += (sin(phaseBass) + 0.3 * sin(phaseBass * 2.0)) * exp(-10.0 * (t % (beatDuration / 2.0))) * 0.30
                    // Lush analog chorus pads
                    phasePad += 2.0 * PI * (root * 2.0) / SAMPLE_RATE
                    sample += (sin(phasePad) + sin(phasePad * 1.004)) * 0.12
                    // Romantic synth melody
                    val melody80s = doubleArrayOf(587.33, 698.46, 880.0, 1046.5, 880.0, 783.99)
                    val mIdx = ((t / beatDuration).toInt()) % melody80s.size
                    phaseLead += 2.0 * PI * melody80s[mIdx] / SAMPLE_RATE
                    sample += sin(phaseLead) * 0.14
                }

                TableTheme.TOKAMAK_REACTOR -> {
                    // Dark Techno / Electromagnetic Reactor C-minor (Cm, Ab, Bb, Gm)
                    val roots = doubleArrayOf(130.81, 103.83, 116.54, 98.0)
                    val root = roots[subBar]

                    // Hypnotic techno sub-kick
                    if (tBeat < 0.16) {
                        phaseKick += 2.0 * PI * (58.0 + 150.0 * exp(-45.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-22.0 * tBeat) * 0.48
                    }
                    // Resonant electromagnetic coil click
                    val tSixteenth = t % (beatDuration / 4.0)
                    if (tSixteenth < 0.03) {
                        val zapFreq = 1800.0 * exp(-80.0 * tSixteenth)
                        sample += sin(2.0 * PI * zapFreq * tSixteenth) * exp(-40.0 * tSixteenth) * 0.18
                    }
                    // Offbeat techno hat
                    val tOffbeat = (t + beatDuration * 0.5) % beatDuration
                    if (tOffbeat < 0.06) {
                        sample += pseudoNoise() * exp(-45.0 * tOffbeat) * 0.18
                    }
                    // Pulsating reactor sub-drone
                    phaseBass += 2.0 * PI * (root * 0.5) / SAMPLE_RATE
                    sample += (sin(phaseBass) + 0.4 * sin(phaseBass * 3.0)) * 0.28
                }

                TableTheme.CYBER_PONG -> {
                    // Vintage 1972 8-Bit Square-Wave Chiptune
                    val roots = doubleArrayOf(220.0, 261.63, 293.66, 329.63) // A3, C4, D4, E4
                    val root = roots[subBar]

                    // 8-bit square wave bass
                    val sIdx = ((t / (beatDuration / 4.0)).toInt()) % 4
                    val bFreq = if (sIdx % 2 == 0) root * 0.5 else root
                    phaseBass += 2.0 * PI * bFreq / SAMPLE_RATE
                    val squareBass = if ((phaseBass % (2.0 * PI)) < PI) 1.0 else -1.0
                    sample += squareBass * 0.22

                    // 8-bit vintage clock / blip drums
                    if (tBeat < 0.04) {
                        val squareKick = if ((sin(2.0 * PI * 80.0 * tBeat)) > 0.0) 1.0 else -1.0
                        sample += squareKick * exp(-30.0 * tBeat) * 0.32
                    }
                    if (beatIndex % 2 == 1 && tBeat < 0.05) {
                        val noiseSquare = if (pseudoNoise() > 0.0) 1.0 else -1.0
                        sample += noiseSquare * exp(-28.0 * tBeat) * 0.20
                    }

                    // Fast 8-bit square arpeggios
                    val pongScale = doubleArrayOf(440.0, 523.25, 659.25, 880.0)
                    val pIdx = ((t / (beatDuration / 8.0)).toInt()) % pongScale.size
                    phaseLead += 2.0 * PI * pongScale[pIdx] / SAMPLE_RATE
                    val squareLead = if ((phaseLead % (2.0 * PI)) < PI) 1.0 else -1.0
                    sample += squareLead * 0.14
                }

                TableTheme.CYBER_COLOSSEUM -> {
                    // Epic Roman Gladiator War March D-minor (Dm, Bb, C, Dm)
                    val roots = doubleArrayOf(146.83, 116.54, 130.81, 146.83)
                    val root = roots[subBar]

                    // Thunderous war drum (deep tom kick)
                    if (tBeat < 0.26) {
                        phaseKick += 2.0 * PI * (40.0 + 80.0 * exp(-18.0 * tBeat)) / SAMPLE_RATE
                        sample += sin(phaseKick) * exp(-10.0 * tBeat) * 0.48
                    }
                    // Martial snare cadence
                    val tTriplet = t % (beatDuration / 3.0)
                    if (tTriplet < 0.08) {
                        sample += (pseudoNoise() * 0.75 + sin(2.0 * PI * 190.0 * tTriplet) * 0.25) * exp(-20.0 * tTriplet) * 0.22
                    }
                    // Regal Roman bronze brass horn chords
                    phaseLead += 2.0 * PI * (root * 2.0) / SAMPLE_RATE
                    phasePad += 2.0 * PI * (root * 3.0) / SAMPLE_RATE
                    val brassWave = (sin(phaseLead) + 0.4 * sin(phaseLead * 2.0) + 0.2 * sin(phasePad)) * 0.22
                    sample += brassWave
                    // Deep war chant bass
                    phaseBass += 2.0 * PI * (root * 0.5) / SAMPLE_RATE
                    sample += sin(phaseBass) * 0.26
                }
            }

            var mixed = sample * 0.46

            // Crossfade suave de 25ms nos limites para loop contínuo e perfeito
            val fadeSamples = (0.025 * SAMPLE_RATE).toInt()
            if (i < fadeSamples) {
                mixed *= (i.toDouble() / fadeSamples)
            } else if (i > numSamples - fadeSamples) {
                mixed *= ((numSamples - i).toDouble() / fadeSamples)
            }

            val clamped = (mixed * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o efeito sonoro de contagem regressiva para cada tema.
     */
    fun generateCountdownWav(file: File, theme: TableTheme, countdownNumber: Int) {
        val durationMs = if (countdownNumber <= 0) 280 else 140
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        var noiseSeed = 554433221L
        fun pseudoNoise(): Double {
            noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFF
            return (noiseSeed.toDouble() / 0x7FFFFFFF) * 2.0 - 1.0
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val decay = exp(-3.0 * progress)

            val sampleVal: Double = when (theme) {
                TableTheme.CLASSIC_NEON -> {
                    val freq = when (countdownNumber) {
                        3 -> 523.25
                        2 -> 659.25
                        1 -> 783.99
                        else -> 1046.50
                    }
                    if (countdownNumber <= 0) {
                        val c6 = sin(2.0 * PI * 1046.50 * t)
                        val e6 = 0.6 * sin(2.0 * PI * 1318.51 * t)
                        val g6 = 0.4 * sin(2.0 * PI * 1567.98 * t)
                        (c6 + e6 + g6) / 2.0 * decay
                    } else {
                        (sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)) / 1.25 * decay
                    }
                }

                TableTheme.CYBER_SOCCER -> {
                    // Apito de árbitro / buzina de estádio
                    if (countdownNumber <= 0) {
                        val whistle = sin(2.0 * PI * (2600.0 + 200.0 * sin(2.0 * PI * 45.0 * t)) * t)
                        val horn = sin(2.0 * PI * 587.33 * t) * 0.6
                        (whistle + horn) / 1.6 * decay
                    } else {
                        val whistleFreq = 2200.0 + (3 - countdownNumber) * 200.0
                        sin(2.0 * PI * (whistleFreq + 150.0 * sin(2.0 * PI * 35.0 * t)) * t) * decay
                    }
                }

                TableTheme.NEO_BASKETBALL -> {
                    // Buzzer digital de 24 segundos
                    val baseFreq = if (countdownNumber <= 0) 660.0 else 380.0 + (3 - countdownNumber) * 60.0
                    val buzz = if ((sin(2.0 * PI * baseFreq * t)) > 0.0) 1.0 else -1.0
                    val tone = sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.4
                    (buzz * 0.6 + tone * 0.4) * decay
                }

                TableTheme.GRAVITY_VOLLEY -> {
                    // Pulso ressonante cósmico zero-g
                    val freq = when (countdownNumber) {
                        3 -> 880.0
                        2 -> 1100.0
                        1 -> 1320.0
                        else -> 1760.0
                    }
                    val fundamental = sin(2.0 * PI * freq * t)
                    val harmonic = 0.3 * sin(2.0 * PI * freq * 2.01 * t)
                    (fundamental + harmonic) / 1.3 * decay
                }

                TableTheme.GRIDIRON_FOOTBALL -> {
                    // Buzina de estádio / foghorn
                    val freq = if (countdownNumber <= 0) 320.0 else 180.0 + (3 - countdownNumber) * 30.0
                    val brass = sin(2.0 * PI * freq * t) + 0.5 * sin(2.0 * PI * freq * 2.0 * t)
                    brass / 1.5 * decay
                }

                TableTheme.CYBER_BASEBALL -> {
                    // Chamada de strike / charge organ
                    val freq = when (countdownNumber) {
                        3 -> 440.0
                        2 -> 554.37
                        1 -> 659.25
                        else -> 880.0
                    }
                    val organ = sin(2.0 * PI * freq * t) + 0.4 * sin(2.0 * PI * freq * 2.0 * t) + 0.2 * sin(2.0 * PI * freq * 3.0 * t)
                    organ / 1.6 * decay
                }

                TableTheme.WASTELAND_APOCALYPSE -> {
                    // Serrilha metálica industrial / alarme apocalíptico
                    val freq = if (countdownNumber <= 0) 440.0 else 220.0 + (3 - countdownNumber) * 40.0
                    val saw = ((2.0 * PI * freq * t % (2.0 * PI)) / PI) - 1.0
                    (saw * 0.7 + pseudoNoise() * 0.3) * decay
                }

                TableTheme.RETRO_SYNTHWAVE -> {
                    // Syndrum 80s pitch drop
                    val startFreq = if (countdownNumber <= 0) 1200.0 else 800.0 + (3 - countdownNumber) * 120.0
                    val instFreq = startFreq * exp(-15.0 * t)
                    sin(2.0 * PI * instFreq * t) * decay
                }

                TableTheme.TOKAMAK_REACTOR -> {
                    // Clique do acelerador de partículas e descarga de plasma
                    val zapFreq = if (countdownNumber <= 0) 1800.0 * exp(-12.0 * t) else 1200.0 * exp(-20.0 * t)
                    sin(2.0 * PI * zapFreq * t) * decay
                }

                TableTheme.CYBER_PONG -> {
                    // Autêntica onda quadrada 8-bit pura
                    val freq = if (countdownNumber <= 0) 880.0 else 440.0
                    (if ((sin(2.0 * PI * freq * t)) > 0.0) 1.0 else -1.0) * decay
                }

                TableTheme.CYBER_COLOSSEUM -> {
                    // Trombeta de bronze romana
                    val freq = when (countdownNumber) {
                        3 -> 220.0
                        2 -> 277.18
                        1 -> 329.63
                        else -> 440.0
                    }
                    val bronze = sin(2.0 * PI * freq * t) + 0.6 * sin(2.0 * PI * freq * 1.5 * t)
                    bronze / 1.6 * decay
                }
            }

            val clamped = (sampleVal * 26000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o som característico de colisão com o rebatedor (Paddle Hit).
     */
    fun generatePaddleHitWav(file: File, theme: TableTheme, mallet: MalletSkin? = null) {
        val durationMs = 65
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        var noiseSeed = 112233445L
        fun pseudoNoise(): Double {
            noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFF
            return (noiseSeed.toDouble() / 0x7FFFFFFF) * 2.0 - 1.0
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = 1.0 - (i.toDouble() / numSamples)

            val sampleVal: Double = when (theme) {
                TableTheme.CLASSIC_NEON -> {
                    // Arcade pop 480Hz
                    sin(2.0 * PI * 480.0 * t) * decay
                }
                TableTheme.CYBER_SOCCER -> {
                    // Chute firme com grave 160Hz e estalo no impacto
                    val sub = sin(2.0 * PI * 160.0 * t) * 0.7
                    val snap = sin(2.0 * PI * 820.0 * t) * exp(-40.0 * t) * 0.5
                    (sub + snap) * decay
                }
                TableTheme.NEO_BASKETBALL -> {
                    // Quique pesado no piso de madeira com ressonância oca
                    val thud = sin(2.0 * PI * 130.0 * t) * 0.8
                    val squeak = sin(2.0 * PI * 950.0 * t) * exp(-50.0 * t) * 0.3
                    (thud + squeak) * decay
                }
                TableTheme.GRAVITY_VOLLEY -> {
                    // Desvio de campo de gravidade zero
                    val whoosh = sin(2.0 * PI * (620.0 * exp(-25.0 * t)) * t)
                    whoosh * decay
                }
                TableTheme.GRIDIRON_FOOTBALL -> {
                    // Impacto de ombreira/tackle com estalo mecânico
                    val clash = pseudoNoise() * exp(-35.0 * t) * 0.5
                    val lowThump = sin(2.0 * PI * 110.0 * t) * 0.7
                    (clash + lowThump) * decay
                }
                TableTheme.CYBER_BASEBALL -> {
                    // Estalo agudo do taco futurista
                    val crack = (sin(2.0 * PI * 1800.0 * t) + pseudoNoise() * 0.4) * exp(-45.0 * t)
                    val wood = sin(2.0 * PI * 320.0 * t) * 0.6
                    (crack * 0.6 + wood * 0.5) * decay
                }
                TableTheme.WASTELAND_APOCALYPSE -> {
                    // Clang de serra circular e metal enferrujado
                    val clang = sin(2.0 * PI * 1450.0 * t) + sin(2.0 * PI * 2900.0 * t) * 0.4
                    val saw = if (sin(2.0 * PI * 380.0 * t) > 0) 0.5 else -0.5
                    (clang * 0.6 + saw * 0.4) * decay
                }
                TableTheme.RETRO_SYNTHWAVE -> {
                    // Tom eletro analógico 80s com slap
                    val analogPop = sin(2.0 * PI * (320.0 * exp(-18.0 * t)) * t)
                    analogPop * decay
                }
                TableTheme.TOKAMAK_REACTOR -> {
                    // Faísca de bobina magnética
                    val zap = sin(2.0 * PI * (980.0 * exp(-30.0 * t)) * t) + pseudoNoise() * exp(-40.0 * t) * 0.3
                    zap * decay
                }
                TableTheme.CYBER_PONG -> {
                    // 1972 Pong blip puro em onda quadrada
                    val square = if (sin(2.0 * PI * 480.0 * t) > 0.0) 1.0 else -1.0
                    square * decay
                }
                TableTheme.CYBER_COLOSSEUM -> {
                    // Choque de escudo de bronze
                    val bronze = sin(2.0 * PI * 620.0 * t) + sin(2.0 * PI * 1240.0 * t) * 0.5
                    bronze * decay
                }
            }

            val clamped = (sampleVal * 25000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o som característico de colisão nas bordas da mesa (Wall Hit).
     */
    fun generateWallHitWav(file: File, theme: TableTheme) {
        val durationMs = 45
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = 1.0 - (i.toDouble() / numSamples)

            val sampleVal: Double = when (theme) {
                TableTheme.CLASSIC_NEON -> sin(2.0 * PI * 240.0 * t) * decay
                TableTheme.CYBER_SOCCER -> sin(2.0 * PI * 200.0 * t) * decay
                TableTheme.NEO_BASKETBALL -> sin(2.0 * PI * 180.0 * t) * decay
                TableTheme.GRAVITY_VOLLEY -> (sin(2.0 * PI * 320.0 * t) + 0.3 * sin(2.0 * PI * 640.0 * t)) * decay
                TableTheme.GRIDIRON_FOOTBALL -> sin(2.0 * PI * 105.0 * t) * decay
                TableTheme.CYBER_BASEBALL -> (sin(2.0 * PI * 650.0 * t) * 0.6 + sin(2.0 * PI * 250.0 * t) * 0.4) * decay
                TableTheme.WASTELAND_APOCALYPSE -> (sin(2.0 * PI * 380.0 * t) + 0.4 * sin(2.0 * PI * 920.0 * t)) * decay
                TableTheme.RETRO_SYNTHWAVE -> sin(2.0 * PI * 350.0 * t) * decay
                TableTheme.TOKAMAK_REACTOR -> (sin(2.0 * PI * 180.0 * t) + 0.4 * sin(2.0 * PI * 540.0 * t)) * decay
                TableTheme.CYBER_PONG -> (if (sin(2.0 * PI * 240.0 * t) > 0.0) 1.0 else -1.0) * decay
                TableTheme.CYBER_COLOSSEUM -> sin(2.0 * PI * 85.0 * t) * decay
            }

            val clamped = (sampleVal * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o som de Ponto Marcado (Ponto Pró / Jogador).
     * Fanfarra triunfante e celebratória exclusiva para cada arena.
     */
    fun generateGoalScoredWav(file: File, theme: TableTheme) {
        val durationMs = 450
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        var noiseSeed = 334455667L
        fun pseudoNoise(): Double {
            noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFF
            return (noiseSeed.toDouble() / 0x7FFFFFFF) * 2.0 - 1.0
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = 1.0 - (i.toDouble() / numSamples)

            val sampleVal: Double = when (theme) {
                TableTheme.CLASSIC_NEON -> {
                    // Fanfarra arpejada C5 -> E5 -> G5 -> C6
                    val freq = if (t < 0.10) 523.25 else if (t < 0.22) 659.25 else if (t < 0.34) 783.99 else 1046.50
                    sin(2.0 * PI * freq * t) * decay
                }
                TableTheme.CYBER_SOCCER -> {
                    // Buzina de gol no estádio e vibração da torcida
                    val horn = sin(2.0 * PI * 587.33 * t) + 0.5 * sin(2.0 * PI * 880.0 * t)
                    val crowd = pseudoNoise() * 0.4
                    (horn * 0.65 + crowd * 0.35) * decay
                }
                TableTheme.NEO_BASKETBALL -> {
                    // Acorde de órgão triunfante da arena de basquete
                    val c5 = sin(2.0 * PI * 523.25 * t)
                    val e5 = sin(2.0 * PI * 659.25 * t) * 0.7
                    val g5 = sin(2.0 * PI * 783.99 * t) * 0.5
                    (c5 + e5 + g5) / 2.2 * decay
                }
                TableTheme.GRAVITY_VOLLEY -> {
                    // Explosão celestial em gravidade zero
                    val freq = 880.0 + 880.0 * (i.toDouble() / numSamples)
                    val crystal = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                    crystal / 1.3 * decay
                }
                TableTheme.GRIDIRON_FOOTBALL -> {
                    // Tiro de canhão de touchdown e sirene
                    val cannon = sin(2.0 * PI * 65.0 * t) * exp(-8.0 * t)
                    val horn = sin(2.0 * PI * 370.0 * t) * 0.6
                    (cannon * 0.6 + horn * 0.4) * decay
                }
                TableTheme.CYBER_BASEBALL -> {
                    // Fanfarra de Home Run no estádio
                    val note = if (t < 0.12) 523.25 else if (t < 0.24) 659.25 else if (t < 0.34) 783.99 else 1046.50
                    (sin(2.0 * PI * note * t) + 0.4 * sin(4.0 * PI * note * t)) / 1.4 * decay
                }
                TableTheme.WASTELAND_APOCALYPSE -> {
                    // Detonação de combustível e acorde industrial
                    val blast = pseudoNoise() * exp(-12.0 * t) * 0.6
                    val heavyTone = sin(2.0 * PI * 185.0 * t) * 0.6
                    (blast + heavyTone) * decay
                }
                TableTheme.RETRO_SYNTHWAVE -> {
                    // Acorde Maj7 80s nostálgico
                    val fmaj7 = sin(2.0 * PI * 349.23 * t) + 0.7 * sin(2.0 * PI * 440.0 * t) + 0.5 * sin(2.0 * PI * 523.25 * t) + 0.4 * sin(2.0 * PI * 659.25 * t)
                    fmaj7 / 2.6 * decay
                }
                TableTheme.TOKAMAK_REACTOR -> {
                    // Ignição e estabilização de fusão nuclear
                    val beamFreq = 440.0 + 600.0 * (i.toDouble() / numSamples)
                    val beam = sin(2.0 * PI * beamFreq * t) + 0.35 * sin(2.0 * PI * beamFreq * 2.0 * t)
                    beam / 1.35 * decay
                }
                TableTheme.CYBER_PONG -> {
                    // Arpejo de vitória chiptune vintage 8-bit
                    val note = if (t < 0.10) 440.0 else if (t < 0.20) 554.37 else if (t < 0.30) 659.25 else 880.0
                    (if (sin(2.0 * PI * note * t) > 0.0) 1.0 else -1.0) * decay
                }
                TableTheme.CYBER_COLOSSEUM -> {
                    // Trombeta de ouro dos gladiadores
                    val note = if (t < 0.14) 293.66 else if (t < 0.28) 369.99 else 440.0
                    val brass = sin(2.0 * PI * note * t) + 0.5 * sin(2.0 * PI * note * 1.5 * t)
                    brass / 1.5 * decay
                }
            }

            val clamped = (sampleVal * 25000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o som de Ponto Sofrido (Ponto Contra / Oponente).
     * Sinal sonoro descendente / alarme característico de cada arena.
     */
    fun generateGoalConcededWav(file: File, theme: TableTheme) {
        val durationMs = 380
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = 1.0 - (i.toDouble() / numSamples)

            val sampleVal: Double = when (theme) {
                TableTheme.CLASSIC_NEON -> {
                    // Sawtooth descendente 330Hz -> 220Hz -> 160Hz
                    val freq = if (t < 0.12) 330.0 else if (t < 0.24) 220.0 else 160.0
                    val saw = ((2.0 * PI * freq * t % (2.0 * PI)) / PI) - 1.0
                    saw * decay
                }
                TableTheme.CYBER_SOCCER -> {
                    // Gemido / buzina dissonante do estádio
                    val tone1 = sin(2.0 * PI * 150.0 * t)
                    val tone2 = sin(2.0 * PI * 158.0 * t) // Frequência de batimento
                    (tone1 + tone2) / 2.0 * decay
                }
                TableTheme.NEO_BASKETBALL -> {
                    // Violação de posse / buzzer estridente de basquete
                    val buzz = if (sin(2.0 * PI * 280.0 * t) > 0.0) 1.0 else -1.0
                    buzz * decay
                }
                TableTheme.GRAVITY_VOLLEY -> {
                    // Colapso de campo de gravidade zero (mergulho sub-bass)
                    val freq = 140.0 * exp(-6.0 * t)
                    sin(2.0 * PI * freq * t) * decay
                }
                TableTheme.GRIDIRON_FOOTBALL -> {
                    // Alarme de falta / turnover
                    val siren = sin(2.0 * PI * (240.0 + 40.0 * sin(2.0 * PI * 10.0 * t)) * t)
                    siren * decay
                }
                TableTheme.CYBER_BASEBALL -> {
                    // Som de strikeout / eliminação
                    val drop = sin(2.0 * PI * (440.0 * exp(-5.0 * t)) * t)
                    drop * decay
                }
                TableTheme.WASTELAND_APOCALYPSE -> {
                    // Falha catastrófica de maquinário
                    val grind = ((2.0 * PI * 140.0 * t % (2.0 * PI)) / PI) - 1.0
                    val rumble = sin(2.0 * PI * 70.0 * t) * 0.5
                    (grind * 0.6 + rumble * 0.4) * decay
                }
                TableTheme.RETRO_SYNTHWAVE -> {
                    // Filtro analógico melancólico fechando
                    val f = 400.0 * exp(-4.0 * t)
                    sin(2.0 * PI * f * t) * decay
                }
                TableTheme.TOKAMAK_REACTOR -> {
                    // Sirene de alarme de contenção / scram do reator
                    val scram = sin(2.0 * PI * (360.0 + 120.0 * sin(2.0 * PI * 6.0 * t)) * t)
                    scram * decay
                }
                TableTheme.CYBER_PONG -> {
                    // Descida quadrada 8-bit clássica de derrota
                    val freq = 220.0 * exp(-5.0 * t)
                    (if (sin(2.0 * PI * freq * t) > 0.0) 1.0 else -1.0) * decay
                }
                TableTheme.CYBER_COLOSSEUM -> {
                    // Trombeta fúnebre do gladiador derrotado
                    val note = if (t < 0.18) 196.0 else 164.81
                    (sin(2.0 * PI * note * t) + 0.4 * sin(2.0 * PI * note * 1.5 * t)) / 1.4 * decay
                }
            }

            val clamped = (sampleVal * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    /**
     * Gera o som base de progressão de pontuação (Score Announcer).
     * O tom sobe proporcionalmente à pontuação durante o jogo via taxa de reprodução harmônica.
     */
    fun generateScoreBlipWav(file: File, theme: TableTheme) {
        val durationMs = 180
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-4.0 * (i.toDouble() / numSamples))

            val sampleVal: Double = when (theme) {
                TableTheme.CYBER_PONG -> {
                    if (sin(2.0 * PI * 523.25 * t) > 0.0) 1.0 else -1.0
                }
                TableTheme.RETRO_SYNTHWAVE, TableTheme.CLASSIC_NEON -> {
                    (sin(2.0 * PI * 523.25 * t) + 0.3 * sin(2.0 * PI * 1046.50 * t)) / 1.3
                }
                TableTheme.GRAVITY_VOLLEY -> {
                    (sin(2.0 * PI * 659.25 * t) + 0.4 * sin(2.0 * PI * 1318.51 * t)) / 1.4
                }
                else -> {
                    sin(2.0 * PI * 587.33 * t)
                }
            } * decay

            val clamped = (sampleVal * 25000.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, SAMPLE_RATE)
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort())
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmData)
        }
    }
}
